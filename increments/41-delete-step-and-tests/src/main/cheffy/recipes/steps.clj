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
              (let [{:keys [recipe-id description sort-order]}
                    (:transit-params request)
                    path-step-id (get-in ctx [:request :path-params :step-id])
                    step-id (or (when path-step-id (parse-uuid path-step-id)) (random-uuid))]
                (assoc ctx :tx-data [{:recipe/recipe-id recipe-id
                                      :recipe/steps [{:step/step-id step-id
                                                      :step/description description
                                                      :step/sort-order sort-order}]}])))
     :leave (fn [ctx]
              (let [path-step-id? (boolean (get-in ctx [:request :path-params :step-id]))]
                (if path-step-id?
                  (assoc ctx :response (rr/status 204))
                  (let [recipe-id (-> ctx :tx-data (first) :recipe/recipe-id)
                        step-id (-> ctx :tx-data (first) :recipe/steps (first) :step/step-id)]
                    (assoc ctx :response (rr/created
                                           (str "/recipes/" recipe-id)
                                           {:step-id step-id}))))))}))

(def upsert-step
  [(bp/body-params)
   http/transit-body
   step-interceptor
   interceptors/transact-interceptor])