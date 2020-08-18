(ns web-programiranje.service.game-service
  (:require [web-programiranje.db.service.db-service :as db-service] ;includes applying database configuration
            [web-programiranje.mapper.mapper :as dto-mapper]))


(defn get-all-games
  "docstring"
  []
  (dto-mapper/to-game-info-list-dto
    (db-service/get-all-games))
  )

(defn get-game-by-id
  "docstring"
  [id]
  (dto-mapper/to-game-dto
    (db-service/get-game-by-id id))
  )
