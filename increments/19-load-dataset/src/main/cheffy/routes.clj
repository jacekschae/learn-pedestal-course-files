(ns cheffy.routes
  (:require [io.pedestal.http.route :as route]))

(defn list-recipes
  [request]
  {:status 200
   :body "list recipes"})

(defn upsert-recipe
  [request]
  {:status 200
   :body "upsert recipes"})

(def routes
  (route/expand-routes
    #{{:app-name :cheffy :schema :http :host "localhost"}
      ["/recipes" :get list-recipes :route-name :list-recipes]
      ["/recipes" :post upsert-recipe :route-name :create-recipe]
      ["/recipes/:recipe-id" :put upsert-recipe :route-name :update-recipe]}))