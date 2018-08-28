(ns clojenetics.logics.trees
  (:require [clojure.tools.logging :as log]
            [clojure.zip :as zip]
            [clojenetics.logics.setters :as setters]
            [clojenetics.logics.terminals :as terminals]
            [clojenetics.logics.utils :refer [abs positive?]]))

(declare create-tree)

(defn create-random-subtree [{:keys [functions] :as state}]
  (let [[func arity] (rand-nth functions)]
    (log/debugf "Recursing tree creation with state: %s" state)
    (cons func (repeatedly arity #(create-tree state)))))

(defn create-tree [{:keys [propagation-technique] :as state}]
  (log/debugf "Doing create-tree with state: %s" state)
  (if (or (nil? propagation-technique)
          (= propagation-technique :random))
    (or (terminals/try-for-terminal state)
        (create-random-subtree (setters/dec-current-tree-depth state)))))

(declare generate-trees)


(defn generate-trees [state]
  (log/infof "%s trees left to generate in this generation" (:seeds-remaining state))
  (if (positive? (:seeds-remaining state))
    (let [tree (create-tree state)
          state (setters/dec-seeds-remaining state)
          state (setters/set-new-tree state tree)]
      (generate-trees state))
    (do (prn "setting scores now")
        (setters/set-scores state))))

(defn do-many-generations [state]
  (log/infof "%s generations left to make" (:generations-remaining state))
  (if (positive? (:generations-remaining state))
    (let [generation (:trees (generate-trees state))
          state (setters/dec-generations state)]
      (do-many-generations state))
    state))

;; 1. Generate initial trees
;; 2. Live for first generation (get scores)
;; 3. Do next generation


; The following code from Lee Spencer at https://gist.github.com/lspector/3398614

(defn tree-depth [i tree]
  (mod (abs i)
       (if (seq? tree)
         (count (flatten tree))
         1)))

(defn subtree-at-index
  [index tree]
  (log/infof "Getting subtree from tree %s at index %s" tree index)
  (let [index (tree-depth index tree)
        zipper (zip/seq-zip tree)]
    (loop [z zipper i index]
      (if (zero? i)
        (zip/node z)
        (if (seq? (zip/node z))
          (recur (zip/next (zip/next z)) (dec i))
          (recur (zip/next z) (dec i)))))))

(defn insert-subtree-at-index
  "Returns a copy of tree with the subtree formerly indexed by
point-index (in a depth-first traversal) replaced by new-subtree."
  [index tree new-subtree]
  (let [index (tree-depth index tree)
        zipper (zip/seq-zip tree)]
    (loop [z zipper i index]
      (if (zero? i)
        (zip/root (zip/replace z new-subtree))
        (if (seq? (zip/node z))
          (recur (zip/next (zip/next z)) (dec i))
          (recur (zip/next z) (dec i)))))))
