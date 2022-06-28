(ns cheffy.conversations
  (:require [io.pedestal.http :as http]
            [cheffy.interceptors :as interceptors]
            [io.pedestal.interceptor :as interceptor]
            [ring.util.response :as rr]
            [io.pedestal.http.body-params :as bp])
  (:import (java.util Date)))

(def conversation-pattern
  [:conversation/conversation-id
   {:conversation/messages
    [:message/message-id
     :message/body
     {:message/owner
      [:account/account-id
       :account/display-name]}]}])

(def find-conversations-by-account-id-interceptor
  (interceptor/interceptor
    {:name ::find-conversations-by-account-id-interceptor
     :enter (fn [ctx]
              (let [db (get-in ctx [:request :system/database :db])
                    account-id (get-in ctx [:request :headers "authorization"])
                    q-data {:query '[:find (pull ?c pattern)
                                     :in $ ?account-id pattern
                                     :where
                                     [?a :account/account-id ?account-id]
                                     [?c :conversation/participants ?a]]
                            :args [db account-id conversation-pattern]}]
                (assoc ctx :q-data q-data)))
     :leave (fn [ctx]
              (let [conversations (mapv first (get ctx :q-result))]
                (assoc ctx :response (rr/response conversations))))}))

(def list-conversations
  [http/transit-body
   interceptors/db-interceptor
   find-conversations-by-account-id-interceptor
   interceptors/query-interceptor])

(def create-message-interceptor
  (interceptor/interceptor
    {:name ::create-message-interceptor
     :enter (fn [ctx]
              (let [from (get-in ctx [:request :headers "authorization"])
                    {:keys [to message-body]} (get-in ctx [:request :transit-params])
                    message-id (random-uuid)
                    path-conversation-id (get-in ctx [:request :path-params :conversation-id])
                    conversation-id (or (when path-conversation-id (parse-uuid path-conversation-id))
                                      (random-uuid))]
                (assoc ctx :tx-data [{:conversation/conversation-id conversation-id
                                      :conversation/participants (mapv #(vector :account/account-id %) [to from])
                                      :conversation/messages (str message-id)}
                                     {:db/id (str message-id)
                                      :message/message-id message-id
                                      :message/owner [:account/account-id from]
                                      :message/read-by [[:account/account-id from]]
                                      :message/body message-body
                                      :message/created-at (Date.)}])))
     :leave (fn [ctx]
              (let [conversation-id (-> ctx :tx-data (first) :conversation/conversation-id)]
                (assoc ctx :response (rr/created
                                       (str "/conversations/" conversation-id)
                                       {:conversation-id conversation-id}))))}))

(def create-message
  [http/transit-body
   (bp/body-params)
   create-message-interceptor
   interceptors/transact-interceptor])