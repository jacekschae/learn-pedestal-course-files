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