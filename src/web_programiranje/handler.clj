(ns web-programiranje.handler
  (:require [compojure.core :refer :all]
            [compojure.route :as route]
            [ring.middleware.defaults :refer [wrap-defaults site-defaults api-defaults]]
            [cheshire.core :as chjson]
            [ring.middleware.json :as ring-json]
            [ring.util.response :as ring-response]
            [web-programiranje.db.service.db-service :as db-service] ;includes applying database configuration
            [web-programiranje.mapper.mapper :as dto-mapper]
            ))


(defroutes app-routes
           (GET "/" request (str request))
           (GET "/test" [] (ring-response/response {:ana "ana" :bara 1}))
           (GET "/user/:id{[0-9]+}" [id] (str "argument " id))
           (GET "/game/:id{[0-9]+}" [id] (chjson/generate-string
                                           (dto-mapper/to-game-dto
                                             (db-service/get-game-by-id id))))
           (route/not-found "Not Found")
           )

(def app
  (-> app-routes
      (wrap-defaults api-defaults)
      (ring-json/wrap-json-response)
      (ring-json/wrap-json-body)
      )
  )
