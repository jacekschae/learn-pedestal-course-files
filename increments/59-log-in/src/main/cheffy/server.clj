(ns cheffy.server
  (:require [com.stuartsierra.component :as component]
            [clojure.edn :as edn]
            [cheffy.components.api-server :as api-server]
            [cheffy.components.database :as database]
            [cheffy.components.auth :as auth]))

(defn create-system
  [config]
  (component/system-map
    :config config
    :auth (auth/service (:auth config))
    :database (database/service (:database config))
    :api-server (component/using
                  (api-server/service (:service-map config))
                  [:database :auth])))

(defn -main
  [config-file]
  (let [config (-> config-file (slurp) (edn/read-string))]
    (component/start (create-system config))))
