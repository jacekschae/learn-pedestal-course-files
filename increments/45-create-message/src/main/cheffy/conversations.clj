(ns cheffy.conversations
  (:require [io.pedestal.http :as http]
            [cheffy.interceptors :as interceptors]
            [io.pedestal.interceptor :as interceptor]
            [ring.util.response :as rr]))

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