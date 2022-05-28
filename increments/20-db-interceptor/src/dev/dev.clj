(ns dev
  (:require [clojure.edn :as edn]
            [cheffy.server :as server]
            [com.stuartsierra.component.repl :as cr]
            [io.pedestal.http :as http]
            [datomic.client.api :as d]))

(defn system [_]
  (-> (-> "src/config/cheffy/development.edn" (slurp) (edn/read-string))
      (server/create-system)))

(cr/set-init system)

(defn start-dev []
  (cr/start))

(defn stop-dev []
  (cr/stop))

(defn restart-dev []
  (cr/reset))

(comment

  (-> cr/system :api-server :service)

  (d/q '[:find ?e ?id
         :where [?e :account/account-id ?id]]
       (d/db (-> cr/system :database :conn)))


  (defn cheffy-interceptors
    [service-map sys-interceptors]
    (let [default-interceptors (-> service-map
                                   (http/default-interceptors)
                                   ::http/interceptors)
          interceptors (into [] (concat
                                  (butlast default-interceptors)
                                  sys-interceptors
                                  [(last default-interceptors)]))]
      (assoc service-map ::http/interceptors interceptors)))

  (start-dev)

  (restart-dev)

  (stop-dev)

  )