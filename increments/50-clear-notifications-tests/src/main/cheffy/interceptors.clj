(ns cheffy.interceptors
  (:require [datomic.client.api :as d]
            [io.pedestal.interceptor :as interceptor]))

(def db-interceptor
  (interceptor/interceptor
    {:name ::db-interceptor
     :enter (fn [ctx]
              (let [conn (get-in ctx [:request :system/database :conn])
                    db (d/db conn)]
                (update-in ctx [:request :system/database] assoc :db db)))}))

(def transact-interceptor
  (interceptor/interceptor
    {:name ::transact-interceptor
     :enter (fn [ctx]
              (let [conn (get-in ctx [:request :system/database :conn])
                    tx-data (get ctx :tx-data)]
                (assoc ctx :tx-result (d/transact conn {:tx-data tx-data}))))}))

(def query-interceptor
  (interceptor/interceptor
    {:name ::query-interceptor
     :enter (fn [ctx]
              (let [q-data (get ctx :q-data)]
                (assoc ctx :q-result (d/q q-data))))}))