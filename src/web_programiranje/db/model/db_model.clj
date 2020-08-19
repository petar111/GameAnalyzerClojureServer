(ns web-programiranje.db.model.db-model
  (:require [toucan.models :as models]))


(models/defmodel Creature :creature)

(models/defmodel PowerStat :creature_stat)

(models/defmodel StatType :stat_type
                 models/IModel
                 (hydration-keys [_] [:stat_type]))

(models/defmodel Stat :stat
                 models/IModel
                 (hydration-keys [_] [:stat]))

; Models
(models/defmodel User :user
                 models/IModel
                 (hydration-keys [_] [:user]))

(models/defmodel Game :game
                 models/IModel
                 (hydration-keys [_] [:game]))

(models/defmodel Player :player_configuration
                 models/IModel
                 (hydration-keys [_] [:player]))

(models/defmodel Strategy :strategy
                 models/IModel
                 (hydration-keys [_] [:strategy :opposing_strategy :selected_strategy]))

(models/defmodel Payoff :payoff)

(models/defmodel GameSession :game_session)

(models/defmodel GameSessionPlayer :game_session_player)

(models/defmodel GameSessionPlayerStrategy :game_session_player_strategy)
