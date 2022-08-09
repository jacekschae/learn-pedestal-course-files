(ns cheffy.recipes.ingredients
  (:require [io.pedestal.http.body-params :as bp]
            [io.pedestal.http :as http]
            [io.pedestal.interceptor :as interceptor]
            [cheffy.interceptors :as interceptors]
            [ring.util.response :as rr]))


(def ingredient-interceptor
  (interceptor/interceptor
    {:name ::ingredient-interceptor
     :enter (fn [{:keys [request] :as ctx}]
              (let [{:keys [recipe-id name amount measure sort-order]}
                    (:transit-params request)
                    path-ingredient-id (get-in ctx [:request :path-params :ingredient-id])
                    ingredient-id (or (when path-ingredient-id (parse-uuid path-ingredient-id)) (random-uuid))]
                (assoc ctx :tx-data [{:recipe/recipe-id recipe-id
                                      :recipe/ingredients [{:ingredient/ingredient-id ingredient-id
                                                            :ingredient/display-name name
                                                            :ingredient/amount amount
                                                            :ingredient/measure measure
                                                            :ingredient/sort-order sort-order}]}])))
     :leave (fn [ctx]
              (let [path-ingredient-id? (boolean (get-in ctx [:request :path-params :ingredient-id]))]
                (if path-ingredient-id?
                  (assoc ctx :response (rr/status 204))
                  (let [recipe-id (-> ctx :tx-data (first) :recipe/recipe-id)
                        ingredient-id (-> ctx :tx-data (first) :recipe/ingredients (first) :ingredient/ingredient-id)]
                    (assoc ctx :response (rr/created
                                           (str "/recipes/" recipe-id)
                                           {:ingredient-id ingredient-id}))))))}))

(def upsert-ingredient
  [(bp/body-params)
   http/transit-body
   ingredient-interceptor
   interceptors/transact-interceptor])

(def retract-ingredient-interceptor
  (interceptor/interceptor
    {:name ::retract-ingredient-interceptor
     :enter (fn [ctx]
              (let [ingredient-id (parse-uuid (get-in ctx [:request :path-params :ingredient-id]))]
                (assoc ctx :tx-data [[:db/retractEntity [:ingredient/ingredient-id ingredient-id]]])))
     :leave (fn [ctx]
              (assoc ctx :response (rr/status 204)))}))

(def delete-ingredient
  [(bp/body-params)
   retract-ingredient-interceptor
   interceptors/transact-interceptor])