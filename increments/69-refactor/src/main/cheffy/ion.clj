(ns cheffy.ion
  (:require [com.stuartsierra.component :as component]
            [cheffy.components.auth :as auth]
            [cheffy.components.database :as database]
            [cheffy.routes :as routes]
            [cheffy.components.api-server :as api-server]
            [io.pedestal.ions :as pi]
            [io.pedestal.http :as http]))


(def config
  {:database {:server-type :dev-local
              :system "api.learnpedestal.com"
              :storage-dir :mem
              :db-name "development"}
   :auth {:client-id "195jdaj3jb5vc2a228b2026chh"
          :client-secret "CLIENT_SECRET"
          :user-pool-id "us-east-1_hhvJzRQtM"
          :jwks "https://cognito-idp.us-east-1.amazonaws.com/us-east-1_hhvJzRQtM"}})

(defn create-provide
  [config]
  (let [system
        (component/system-map
          :config config
          :auth (auth/service (:auth config))
          :database (database/service (:database config)))

        {:keys [database auth]}
        (component/start system)

        provider
        (-> {:env :prod
             ::http/routes (routes/routes)
             ::http/chain-provider pi/ion-provider}
          (api-server/cheffy-interceptors [(api-server/inject-system {:system/database database :system/auth auth})])
          (http/create-provider))]
    provider))

(def app
  (create-provide config))

(defn handler
  [req]
  (app req))