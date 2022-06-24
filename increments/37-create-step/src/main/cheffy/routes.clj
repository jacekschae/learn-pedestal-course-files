(ns cheffy.routes
  (:require [io.pedestal.http.route :as route]
            [cheffy.recipes :as recipes]))


(defn routes
  []
  (route/expand-routes
    #{["/recipes" :get recipes/list-recipes :route-name :list-recipes]
      ["/recipes" :post recipes/create-recipe :route-name :create-recipe]
      ["/recipes/:recipe-id" :get recipes/retrieve-recipe :route-name :retrieve-recipe]
      ["/recipes/:recipe-id" :put recipes/update-recipe :route-name :update-recipe]
      ["/recipes/:recipe-id" :delete recipes/delete-recipe :route-name :delete-recipe]}))