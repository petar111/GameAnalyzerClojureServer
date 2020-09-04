(ns web-programiranje.handler
  (:require [compojure.core :refer :all]
            [compojure.route :as route]
            [ring.middleware.defaults :refer [wrap-defaults site-defaults api-defaults]]
            [cheshire.core :as chjson]
            [ring.middleware.json :as ring-json]
            [ring.util.response :as ring-response]
            [ring.middleware.cors :as ring-cors]
            [web-programiranje.service.game-service :as game-service]
            [web-programiranje.service.auth-service :as auth-service]
            [web-programiranje.service.user-service :as user-service]
            [web-programiranje.security.jwt-token-provider :as jwt-token-provider]
            )
  (:import (java.sql Date)))



(def cors-exposed-headers ["Origin" "Content-Type" "Accept" "Jwt-Token" "Authorization"
                           "Access-Control-Allow-Origin" "Access-Control-Allow-Credentials" "WWW-Authenticate"])
(def cors-allowed-methods [:get :post])

(defn make-response
  "docstring"
  [response-dto]
  (->(ring-response/response (chjson/generate-string response-dto))
     )
  )

(defn make-response-with-authentication [dto-response jwt-token]
  (-> (make-response dto-response)
      (ring-response/header "Jwt-token" jwt-token)
      )
  )



(defn make-error-response [exception status]
  (.printStackTrace exception)
  (-> (ring-response/response (.getMessage exception))
      (ring-response/header "WWW-Authenticate" "Jwt-token realm=\"Application for game analyzing\", charset=\"UTF-8\"")
      (ring-response/status status))
  )

(defn make-authentication-failed-response [exception status]
  (-> (ring-response/response (.getMessage exception))
      (ring-response/header "WWW-Authenticate" "Jwt-token realm=\"Application for game analyzing\", charset=\"UTF-8\"")
      (ring-response/status status))
  )

(defn handle-login [login-request]
  (try
    (let [login-signal (auth-service/login login-request)]
      (let [jwt-token (jwt-token-provider/generate-jwt-token (assoc jwt-token-provider/claim :sub (:username (:user login-signal))))]
        (make-response-with-authentication (:user login-signal) jwt-token)
        )
      )
    (catch Exception e (make-error-response e 401))
    )
  )

(defn handle-register
  "docstring"
  [user]
  (try
    (let [register-signal (auth-service/register-user user)]
      (let [jwt-token (jwt-token-provider/generate-jwt-token (assoc jwt-token-provider/claim :sub (:username (:user register-signal))))]
        (make-response-with-authentication (:user register-signal) jwt-token)
        )
      )
    (catch Exception e (make-error-response e 401))
    )
  )



(defn handle-request-deprecated
  "docstring"
  [handle]
  (try
    (jwt-token-provider/validate-jwt-token "eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzUxMiJ9.eyJhdWQiOiJBdWRpZW5jZSBGSVhNRSIsInN1YiI6InBldGFyIiwiaXNzIjoiU0lVWCIsImV4cCI6MTU5OTI2NDI2NiwiaWF0IjoxNTk4ODMyMjY2LCJhdXRob3JpdGllcyI6W119.Fnip_-VYI_GpvSLfnu1nVimcz1HFfDPWEw0p2GfBc8XQNypaqXawh16Mq1C6y_oQSnZGpOSHjKdtikRowoxr1w")
    (make-response (handle))
    (catch Exception e (make-error-response e 400))
    )
  )



(defn handle-request
  "docstring"
  [handle request]
  (try
    (let [token-validation (jwt-token-provider/validate-jwt-token (get (:headers request) "authorization"))]
      (if (= (:signal token-validation) "OK")
        (make-response (handle))
        (throw (Exception. "Token validation failed."))
        )
      )
    (catch Exception e
      (if (.startsWith (.getMessage e) "Authentication failed.")
        (make-authentication-failed-response e 401)
        (make-error-response e 400)
        )
      )
    )
  )


(defroutes app-routes
           (GET "/" request (str request))
           (GET "/user/:id" request (handle-request (partial user-service/get-user-by-id (:id (:params request))) request))
           (GET "/user/:id/followers" request (handle-request (partial user-service/get-user-followers-usernames-by-user-id (:id (:params request)) ) request))
           (GET "/user/:id/following" request (handle-request (partial user-service/get-user-following-usernames-by-user-id (:id (:params request)) ) request))
           (GET "/user/:id/followers/count" request (handle-request (partial user-service/get-followers-count-by-id (:id (:params request)) ) request))
           (GET "/user/:id/following/count" request (handle-request (partial user-service/get-following-count-by-id (:id (:params request)) ) request))
           (GET "/user/:id/games" request (handle-request (partial game-service/get-games-by-user-id (:id (:params request)) ) request))
           (GET "/user/:id/is-following/:following" request (handle-request (partial user-service/get-is-user-following (:id (:params request)) (:following (:params request))) request))
           (GET "/user/@/:username" request (handle-request (partial user-service/get-user-by-username (:username (:params request)) ) request))
           (POST "/user/follow" request (handle-request (partial user-service/follow-user (:body request)) request))
           (POST "/user/un-follow" request (handle-request (partial user-service/un-follow-user (:body request)) request))
           (POST "/register" request (handle-register (:body request)))
           (POST "/user/update" request (handle-request (partial auth-service/update-user (:body request)) request))
           (POST "/user/update/experience" request  (handle-request (partial user-service/update-user-experience (:user (:body request)) (:experience (:body request))) request))
           (GET "/game/:id/advice" request (handle-request (partial game-service/get-game-advice-by-id (:id (:params request)) ) request))
           (POST "/game/request-verification" request (handle-request (partial game-service/request-verification (:body request)) request))
           (GET "/game/:id{[0-9]+}" request (handle-request (partial game-service/get-game-by-id (:id (:params request)) ) request))
           (GET "/game/score/get" request (handle-request (partial game-service/get-game-scores (:user_id (:params request)) (:game_id (:params request)) ) request))
           (GET "/game/scores-today" request (handle-request (partial game-service/get-game-scores-by-date (Date. (System/currentTimeMillis)) 5) request))
           (POST "/game/score/submit" request (handle-request (partial game-service/insert-game-score (:body request)) request))
           (GET "/game/all" request (handle-request (partial game-service/get-all-games (:page (:params request)) (:pageSize (:params request))) request))
           (GET "/game/all/count" request (handle-request (partial game-service/get-all-games-count) request))
           (POST "/login" request (handle-login (:body request)))
           (POST "/game/insert" request (handle-request (partial game-service/insert (:body request)) request))
           (GET "/game/get" request (handle-request (partial game-service/get-by-name (:name (:params request))) request))
           (GET "/game/game-session/get-by-creator" request (handle-request (partial game-service/get-game-session-infos-by-creator-username (:username (:params request))) request)) ;this one and one under are somehow in conflict if i put this one under
           (GET "/game/game-session/:id" request (handle-request (partial game-service/get-game-session-by-id (:id (:params request)) ) request))
           (POST "/game/game-session/save" request (handle-request (partial game-service/save-game-session (:body request)) request))
           (route/not-found "Not Found")
           )

(def app
  (-> app-routes
      (wrap-defaults api-defaults)
      (ring-json/wrap-json-response)
      (ring-json/wrap-json-body {:keywords? true :bigdecimals? true})
      (ring-cors/wrap-cors :access-control-allow-origin #"http://localhost:4200" :access-control-expose-headers cors-exposed-headers :access-control-allow-methods cors-allowed-methods)
      )
  )
