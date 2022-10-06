(ns cheffy.account
  (:require [cheffy.components.auth :as auth]
            [ring.util.response :as rr]
            [io.pedestal.http :as http]
            [io.pedestal.http.body-params :as bp]
            [cheffy.interceptors :as interceptors]))

(def sign-up-interceptor
  {:name ::sign-up-interceptor
   :enter (fn [{:keys [request] :as ctx}]
            (let [create-cognito-account (auth/create-cognito-account
                                           (:system/auth request)
                                           (:transit-params request))]
              (assoc ctx :tx-data create-cognito-account)))
   :leave (fn [ctx]
            (let [account-id (-> ctx :tx-data (first) :account/account-id)]
              (assoc ctx :response (rr/response {:account-id account-id}))))})

(def sign-up
  [http/transit-body
   (bp/body-params)
   sign-up-interceptor
   interceptors/transact-interceptor])

(def confirm-account-interceptor
  {:name ::confirm-account-interceptor
   :enter (fn [{:keys [request] :as ctx}]
            (auth/confirm-cognito-account
              (:system/auth request)
              (:transit-params request))
            ctx)
   :leave (fn [ctx]
            (assoc ctx :response (rr/status 204)))})

(def confirm
  [http/transit-body
   (bp/body-params)
   confirm-account-interceptor])

(defn log-in-response
  [request]
  (let [cognito-log-in (auth/cognito-log-in
                                 (:system/auth request)
                                 (:transit-params request))]
    (rr/response cognito-log-in)))

(def log-in
  [http/transit-body
   (bp/body-params)
   log-in-response])

(defn refresh-token-response
  [request]
  (let [{:keys [refresh-token]} (:transit-params request)
        sub (get-in request [:claims "sub"])
        cognito-refresh-token (auth/cognito-refresh-token
                                (:system/auth request)
                                {:refresh-token refresh-token :sub sub})]
    (rr/response cognito-refresh-token)))


(def refresh-token
  [http/transit-body
   (bp/body-params)
   interceptors/verify-json-web-token
   refresh-token-response])

(defn update-role-response
  [request]
  (auth/cognito-update-role
    (:system/auth request)
    (:claims request))
  (rr/status 200))


(def update-role
  [(bp/body-params)
   http/transit-body
   interceptors/verify-json-web-token
   update-role-response])

(defn delete-user-response
  [request]
  (auth/cognito-delete-user
    (:system/auth request)
    (:claims request))
  (rr/status 200))


(def delete-account
  [(bp/body-params)
   http/transit-body
   interceptors/verify-json-web-token
   delete-user-response])