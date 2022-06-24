(ns cheffy.recipes.steps
  (:require [io.pedestal.http.body-params :as bp]
            [io.pedestal.http :as http]
            [io.pedestal.interceptor :as interceptor]
            [cheffy.interceptors :as interceptors]
            [ring.util.response :as rr]))


(def step-interceptor
  (interceptor/interceptor
    {:name ::step-interceptor
     :enter (fn [{:keys [request] :as ctx}]
              (let [{:keys [recipe-id step-id description sort-order] :or {step-id (random-uuid)}}
                    (:transit-params request)]
                (assoc ctx :tx-data [{:recipe/recipe-id recipe-id
                                      :recipe/steps [{:step/step-id step-id
                                                      :step/description description
                                                      :step/sort-order sort-order}]}])))
     :leave (fn [ctx]
              (let [recipe-id (-> ctx :tx-data (first) :recipe/recipe-id)
                    step-id (-> ctx :tx-data (first) :recipe/steps (first) :step/step-id)]
                (assoc ctx :response (rr/created
                                       (str "/recipes/" recipe-id)
                                       {:step-id step-id}))))}))

(def create-step
  [(bp/body-params)
   http/transit-body
   step-interceptor
   interceptors/transact-interceptor])