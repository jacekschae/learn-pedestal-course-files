(ns cheffy.conversations
  (:require [io.pedestal.http :as http]
            [cheffy.interceptors :as interceptors]
            [io.pedestal.interceptor :as interceptor]
            [ring.util.response :as rr]
            [io.pedestal.http.body-params :as bp]
            [datomic.client.api :as d])
  (:import (java.util Date)))

(def conversation-pattern
  [:conversation/conversation-id
   {:conversation/messages
    [:message/message-id
     :message/body
     {:message/owner
      [:account/account-id
       :account/display-name]}]}])

(defn find-unread-messages
  [{:keys [db]} {:keys [account-id conversation-id]}]
  (->> (d/q '[:find ?m
              :in $ ?account-id ?conversation-id
              :where
              [?a :account/account-id ?account-id]
              [?c :conversation/conversation-id ?conversation-id]
              [?c :conversation/participants ?a]
              [?c :conversation/messages ?m]
              (not [?m :message/read-by ?a])]
         db account-id conversation-id)
    (map first)))

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

(def find-messages-by-conversation-id-interceptor
  (interceptor/interceptor
    {:name ::find-messages-by-conversation-id-interceptor
     :enter (fn [ctx]
              (let [db (get-in ctx [:request :system/database :db])
                    conversation-id (parse-uuid (get-in ctx [:request :path-params :conversation-id]))
                    message-pattern [:message/message-id
                                     :message/body
                                     :message/created-at
                                     {:message/owner
                                      [:account/account-id
                                       :account/display-name]}]]
                (assoc ctx :q-data {:query '[:find (pull ?m pattern)
                                             :in $ ?conversation-id pattern
                                             :where
                                             [?e :conversation/conversation-id ?conversation-id]
                                             [?e :conversation/messages ?m]]
                                    :args [db conversation-id message-pattern]})))
     :leave (fn [ctx]
              (let [messages (->> (get ctx :q-result) (map first) (sort-by :message/created-at))]
                (assoc ctx :response (rr/response messages))))}))

(def list-messages
  [http/transit-body
   interceptors/db-interceptor
   find-messages-by-conversation-id-interceptor
   interceptors/query-interceptor])

(def read-messages-interceptor
  (interceptor/interceptor
    {:name ::read-messages-interceptor
     :enter (fn [ctx]
              (let [db (get-in ctx [:request :system/database :db])
                    conversation-id (parse-uuid (get-in ctx [:request :path-params :conversation-id]))
                    account-id (get-in ctx [:request :headers "authorization"])
                    unread-messages (find-unread-messages
                                      {:db db}
                                      {:account-id account-id
                                       :conversation-id conversation-id})]
                (if (seq unread-messages)
                  (let [tx-data (for [message unread-messages]
                                  [:db/add message :message/read-by [:account/account-id account-id]])]
                    (assoc ctx :tx-data tx-data))
                  ctx)))
     :leave (fn [ctx]
              (assoc ctx :response (rr/status 204)))}))

(def clear-notifications
  [interceptors/db-interceptor
   read-messages-interceptor
   interceptors/transact-interceptor])