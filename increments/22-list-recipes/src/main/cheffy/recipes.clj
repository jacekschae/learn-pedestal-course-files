(ns cheffy.recipes
  (:require [cheffy.interceptors :as interceptors]))



(defn list-recipes-response
  [request]
  {:status 200
   :body "list recipes"})

(def list-recipes
  [interceptors/db-interceptor
   list-recipes-response])

(defn upsert-recipe-response
  [request]
  {:status 200
   :body "upsert recipes"})