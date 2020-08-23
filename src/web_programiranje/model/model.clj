(ns web-programiranje.model.model)

(defrecord creature [id name diet-type power-stats])

(defrecord power-stat [id stat amount])

(defrecord stat [id name stat-type])

(defrecord stat-type [id name])

(defrecord game [id name externalInfo description creator strategies players verificationStatus])

(defrecord game-info [id name externalInfo description creatorUsername verificationStatus])

(defrecord player [id name payoffs playableStrategies])

(defrecord payoff [id amount playedStrategy opposingStrategy])

(defrecord strategy [id name])

(defrecord user [id firstName lastName username email password country dateOfBirth
                 isAccountNonLocked isCredentialsNonExpired isEnabled isAccountNonExpired experience rank
                 followersCount followingCount numberOfVerifiedGames])

(defrecord game-session [id creator numberOfRounds game players])

(defrecord game-session-info [id numberOfRounds game])

(defrecord game-session-player [id player totalPayoff selectedStrategy playedStrategies playerLabel])

(defrecord game-session-player-strategy [id timesPlayed strategy])

(defrecord game-advice-data [nashEquilibria])

(defrecord game-score [id totalPayoff numberOfRounds game user dateCreated])

(defrecord rank [id name experienceMin experienceMax])

(def attack (->stat-type 1 "ATTACK"))
(def slash (->stat 1 "Slash" attack))
(def power-stat-slash (->power-stat 1 slash 44.0))
(def power-stat-slash2 (->power-stat 2 slash 55.0))

(def manticore (->creature 1 "Manticore" "omnivore" (cons power-stat-slash2
                                                                (cons power-stat-slash '()))))