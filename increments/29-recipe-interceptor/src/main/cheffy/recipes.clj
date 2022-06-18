(ns cheffy.recipes
  (:require [cheffy.interceptors :as interceptors]
            [datomic.client.api :as d]
            [ring.util.response :as rr]
            [io.pedestal.http :as http]
            [io.pedestal.http.body-params :as bp]))

(def recipe-pattern
  [:recipe/recipe-id
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
     :ingredient/sort-order]}])

(defn query-result->recipe
  [[{:account/keys [_favorite-recipes] :as recipe}]]
  (-> recipe
    (assoc :recipe/favorite-count (count _favorite-recipes))
    (dissoc :account/_favorite-recipes)))

(defn list-recipes-response
  [request]
  (let [db (get-in request [:system/database :db])
        account-id (get-in request [:headers "authorization"])
        public-recipes (mapv query-result->recipe
                         (d/q '[:find (pull ?e pattern)
                                :in $ pattern
                                :where [?e :recipe/public? true]]
                           db recipe-pattern))]
    (if account-id
      (let [drafts-recipes (mapv query-result->recipe
                             (d/q '[:find (pull ?e pattern)
                                    :in $ ?account-id pattern
                                    :where
                                    [?owner :account/account-id ?account-id]
                                    [?e :recipe/owner ?owner]
                                    [?e :recipe/public? false]]
                               db account-id recipe-pattern))]
        (rr/response {:public public-recipes
                      :drafts drafts-recipes}))
      (rr/response {:public public-recipes}))))

(def list-recipes
  [interceptors/db-interceptor
   http/transit-body
   list-recipes-response])

(defn create-recipe-response
  [request]
  (let [account-id (get-in request [:headers "authorization"])
        recipe-id (random-uuid)
        {:keys [name public prep-time img]} (get-in request [:transit-params])
        conn (get-in request [:system/database :conn])]
    (d/transact conn {:tx-data [{:recipe/recipe-id recipe-id
                                 :recipe/display-name name
                                 :recipe/public? public
                                 :recipe/prep-time prep-time
                                 :recipe/image-url img
                                 :recipe/owner [:account/account-id account-id]}]})
    (rr/created (str "/recipes/" recipe-id) {:recipe-id recipe-id})))

(def create-recipe
  [(bp/body-params)
   http/transit-body
   create-recipe-response])

(defn retrieve-recipe-response
  [request]
  (let [db (get-in request [:system/database :db])
        recipe-id (parse-uuid (get-in request [:path-params :recipe-id]))]
    (rr/response
      (d/q '[:find (pull ?e pattern)
             :in $ ?recipe-id pattern
             :where [?e :recipe/recipe-id ?recipe-id]]
        db recipe-id recipe-pattern))))

(def retrieve-recipe
  [interceptors/db-interceptor
   http/transit-body
   retrieve-recipe-response])

(defn update-recipe-response
  [request]
  (let [account-id (get-in request [:headers "authorization"])
        recipe-id (parse-uuid (get-in request [:path-params :recipe-id]))
        {:keys [name public prep-time img]} (get-in request [:transit-params])
        conn (get-in request [:system/database :conn])]
    (d/transact conn {:tx-data [{:recipe/recipe-id recipe-id
                                 :recipe/display-name name
                                 :recipe/public? public
                                 :recipe/prep-time prep-time
                                 :recipe/image-url img
                                 :recipe/owner [:account/account-id account-id]}]})
    (rr/response 204)))

(def update-recipe
  [(bp/body-params)
   update-recipe-response])


(defn delete-recipe-response
  [request]
  (let [recipe-id (parse-uuid (get-in request [:path-params :recipe-id]))
        conn (get-in request [:system/database :conn])]
    (d/transact conn {:tx-data [[:db/retractEntity [:recipe/recipe-id recipe-id]]]})
    (rr/response 204)))

(def delete-recipe
  [delete-recipe-response])