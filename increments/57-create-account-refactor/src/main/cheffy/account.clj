(ns cheffy.account
  (:require [cheffy.components.auth :as auth]
            [ring.util.response :as rr]
            [io.pedestal.http :as http]
            [io.pedestal.http.body-params :as bp]
            [cheffy.interceptors :as interceptors]
            [io.pedestal.interceptor.chain :as chain]))

(def sign-up-interceptor
  {:name ::sign-up-interceptor
   :enter (fn [{:keys [request] :as ctx}]
            (let [create-cognito-account (auth/create-cognito-account
                                           (:system/auth request)
                                           (:transit-params request))
                  email (get-in request [:transit-params :email])]
              (if-not (contains? create-cognito-account :cognitect.anomalies/category)
                (-> ctx
                  (chain/enqueue interceptors/transact-interceptor)
                  (assoc :tx-data [{:account/account-id (:UserSub create-cognito-account)
                                        :account/display-name email}]))
                (assoc ctx :response (rr/bad-request {:type (:__type create-cognito-account)
                                                      :message (:message create-cognito-account)
                                                      :data {:account-id email}})))))})

(def sign-up
  [http/transit-body
   (bp/body-params)
   sign-up-interceptor])