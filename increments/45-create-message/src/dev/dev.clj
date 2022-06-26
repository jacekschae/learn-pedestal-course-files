(ns dev
  (:require [clojure.edn :as edn]
            [cheffy.server :as server]
            [com.stuartsierra.component.repl :as cr]
            [io.pedestal.http :as http]
            [io.pedestal.test :as pt]
            [datomic.client.api :as d]
            [cognitect.transit :as transit])
  (:import (java.io ByteArrayOutputStream ByteArrayInputStream)))

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

  (-> (transit-write {:name "name"
                      :public true
                      :prep-time 30
                      :img "https://github.com/clojure.png"})
    (transit-read))


  (let [db (d/db (-> cr/system :database :conn))
        recipe-pattern [:recipe/recipe-id
                        :recipe/prep-time
                        :recipe/display-name
                        :recipe/image-url
                        :recipe/public?
                        :account/_favorite-recipes
                        {:recipe/owner
                         [:account/account-id
                          :account/display-name]}
                        {:recipe/steps
                         [:step/step-id
                          :step/description
                          :step/sort-order]}
                        {:recipe/ingredients
                         [:ingredient/ingredient-id
                          :ingredient/display-name
                          :ingredient/amount
                          :ingredient/measure
                          :ingredient/sort-order]}]]
    (mapv query-result->recipe (d/q '[:find (pull ?e pattern)
                                      :in $ pattern
                                      :where [?e :recipe/public? false]]
                                 db recipe-pattern)))



  (pt/response-for
    (-> cr/system :api-server :service ::http/service-fn)
    :get "/recipes"
    :headers {"Authorization" "auth|5fbf7db6271d5e0076903601"
              "Content-Type" "application/transit+json"})

  (pt/response-for
    (-> cr/system :api-server :service ::http/service-fn)
    :post "/recipes"
    :headers {"Authorization" "auth|5fbf7db6271d5e0076903601"
              "Content-Type" "application/transit+json"}
    :body (transit-write {:name "name"
                          :public true
                          :prep-time 30
                          :img "https://github.com/clojure.png"}))


  (let [conversation-pattern [:conversation/conversation-id
                              {:conversation/messages
                               [:message/message-id
                                :message/body
                                {:message/owner
                                 [:account/account-id
                                  :account/display-name]}]}]]
    (d/q '[:find (pull ?c pattern)
           :in $ ?account-id pattern
           :where
           [?a :account/account-id ?account-id]
           [?c :conversation/participants ?a]]
      (d/db (-> cr/system :database :conn)) "auth|5fbf7db6271d5e0076903601" conversation-pattern))


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