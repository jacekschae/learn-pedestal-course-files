(ns cheffy.routes
  (:require [io.pedestal.http.route :as route]
            [io.pedestal.http :as http]
            [cheffy.recipes :as recipes]))



(def routes
  (route/expand-routes
    #{{:app-name :cheffy :schema :http ::http/host "localhost"}
      ["/recipes" :get recipes/list-recipes :route-name :list-recipes]
      ["/recipes" :post recipes/upsert-recipe-response :route-name :create-recipe]
      ["/recipes/:recipe-id" :put recipes/upsert-recipe-response :route-name :update-recipe]}))