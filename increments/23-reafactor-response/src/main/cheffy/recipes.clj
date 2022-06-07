(ns cheffy.recipes
  (:require [cheffy.interceptors :as interceptors]
            [datomic.client.api :as d]
            [ring.util.response :as rr]))

(defn list-recipes-response
  [request]
  (let [db (get-in request [:system/database :db])
        account-id (get-in request [:headers "authorization"])
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
                          :ingredient/sort-order]}]
        public-recipes (d/q '[:find (pull ?e pattern)
                              :in $ pattern
                              :where [?e :recipe/public? true]]
                         db recipe-pattern)]
    (if account-id
      (let [drafts-recipes (d/q '[:find (pull ?e pattern)
                                  :in $ ?account-id pattern
                                  :where
                                  [?owner :account/account-id ?account-id]
                                  [?e :recipe/owner ?owner]
                                  [?e :recipe/public? false]]
                             db account-id recipe-pattern)]
        (rr/response {:public public-recipes
                      :drafts drafts-recipes}))
      (rr/response {:public public-recipes}))))

(def list-recipes
  [interceptors/db-interceptor list-recipes-response])

(defn upsert-recipe-response
  [request]
  {:status 200
   :body "upsert recipes"})