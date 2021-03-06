(ns clojenetics.logics.generations
  (:require [taoensso.timbre :refer [debug debugf]]
            [clojenetics.logics.utils :as utils]
            [clojenetics.logics.setters :as setters]
            [clojenetics.logics.trees :as trees]))

(defn raw-fitness-as-error [target fn]
  (debugf "Calculating raw fitness (error) from fn %s and target %s"
          fn target)
  (try
    (utils/abs (- target (eval fn)))
    (catch ArithmeticException e
      :ERROR)))

(defn standardized-fitness
  ([raw-fitness] (standardized-fitness raw-fitness 0))
  ([raw-fitness max-raw-fitness]
   (utils/abs (- max-raw-fitness raw-fitness))))

(defn adjusted-fitness [standardized-fitness]
  (/ 1 (+ 1 standardized-fitness)))

(declare generate-trees)

(defn do-generation [state]
  (debugf "%s trees to generate in this generation" (:seeds-remaining state))
  (if (utils/strictly-positive? (:seeds-remaining state))
    (let [tree (trees/create-tree state)
          state (setters/dec-seeds-remaining state)
          state (setters/set-new-tree state tree)]
      (do-generation state))
    (setters/set-scores state)))

(defn do-many-generations [state]
  (debugf "%s generations remaining" (:generations-remaining state))
  (if (utils/strictly-positive? (:generations-remaining state))
    (-> state do-generation setters/dec-generations do-many-generations)
    (setters/set-best-tree state)))