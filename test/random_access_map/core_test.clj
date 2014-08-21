(ns random-access-map.core-test
  (:require [clojure.test :refer :all]
            [clojure.core.match :refer [match]]
            [random-access-map.core :refer :all]))

(defn- violates-red-invariant?
  "Determines whether there are any red-red parent-child pairs in the tree."
  [tree]
  (if (ram-empty? tree)
    false
    (if (and (= (color tree) :red)
             (or (and (not (ram-empty? (ltree tree)))
                      (= :red (color (ltree tree))))
                 (and (not (ram-empty? (rtree tree)))
                      (= :red (color (rtree tree))))))
      true
      (or (violates-red-invariant? (ltree tree))
          (violates-red-invariant? (rtree tree))))))

(defn- height
  "Computes the height of a tree."
  [tree]
  (if (ram-empty? tree)
    0
    (max (inc (height (ltree)))
         (inc (height (rtree))))))

(defn- black-height
  "Computes the black height of a tree."
  [tree]
  (if (ram-empty? tree)
    1
    (let [node-count
          (if (= :black (color tree))
            1
            0)]
      (+ node-count
         (max (black-height (ltree tree))
              (black-height (rtree tree)))))))

(defn- violates-black-invariant?
  "Determines whether tree has unequal black heights for its branches."
  [tree]
  (let [t (.tree tree)]
  (if (ram-empty? t)
    false
    (if (= (black-height (ltree t)) (black-height (rtree t)))
      false
      true))))

(defn- balanced?
  "Determines if a red-black tree is balanced"
  [tree]
  (not (or (violates-red-invariant? tree)
           (violates-black-invariant? tree))))

(defn i [a b]
  (assoc a b (keyword (str b)))

(defn r [a b]
  (dissoc a (keyword (str b))))

(def inserted-in-order
  (reduce i (->RandomAccessMap) (range 10)))

(def inserted-in-reverse-order
  (reduce i (->RandomAccessMap) (range 9 -1 -1)))

(def inserted-in-wierd-order
  (reduce i (->RandomAccessMap) [5 4 6 3 7 2 8 1 9 0 10]))

(def large-set
  (reduce i (->RandomAccessMap) (range 100)))

(def large-reverse-set
  (reduce i large-set (range 99 -1 -1)))

(def first-removed-tree
  (reduce r large-set (range 2 50 2)))

(def second-removed-tree
  (reduce r inserted-in-order [1 3 5 6]))

(def even-removed
  (reduce
   r
   (reduce i (->RandomAccessMap) (range 10))
   [0 2 4 6 8]))

(defn valid-colors?
  "Checks to see if all colors are either red or black."
  [tree]
  (if (keyword? tree)
    (= tree :black-leaf)
    (let [[c l k v s r] tree]
      (and (or (= :black c) (= :red c))
           (valid-colors? l)
           (valid-colors? r)))))
(deftest random-access-map-insert-balanced
  "Testing for proper balancing on insert"
  (testing "Inserting a list in ascending order from 1 to 10..."
    (is (balanced? inserted-in-order)))
  (testing "Inserting a list in descending order from 10 to 1..."
    (is (balanced? inserted-in-reverse-order)))
  (testing "Inserting a list in a wierd, but orderly, order..."
    (is (balanced? inserted-in-wierd-order)))
  (testing "Checking an empty set for balance..."
    (is (balanced? (empty-ram))))
  (testing "Checking for balance of a one-item set..."
    (is (balanced? (i (empty-ram) 1))))
  (testing "Some good stuff: Inserting a list in ascending order from 1 to 100..."
    (is (balanced? large-set)))
  (testing "Some good stuff: Inserting a list in descending order from 100 to 1..."
    (is (balanced? large-reverse-set))))

(deftest random-access-map-remove-balanced
  "Testing the remove part of this"
    (testing "Removed even numbers up to 50 from set of 100..."
      (is (balanced? first-removed-tree)))
    (testing "Removed some numbers but not others from set of 20..."
      (is (balanced? second-removed-tree)))
    (testing "Removing from a list of 10..."
      (is (balanced? even-removed)))
    (testing "Removing from the empty list..."
      (is (= (r (empty-ram) 1) (empty-ram))))
    (testing "Removing element that doesn't exist..."
      (is (= first-removed-tree (r first-removed-tree 101)))))

(deftest random-access-map-remove-colors
  "Checks colors are correct after removal."
    (testing "Checking colors on first tree..."
      (is (valid-colors? first-removed-tree)))
    (testing "Checking colors on second removed tree..."
      (is (valid-colors? second-removed-tree)))
    (testing "Checking colors on even removed tree..."
      (is (valid-colors? even-removed))))

(defn- actual-count [tree]
  (match [tree]
         [:black-leaf] 0
         [:double-black-leaf] 0
         [[c l k v s r]] (+ (actual-count l) (actual-count r) 1)
         :else
         (ex-info "Actual count called on a non-tree."
                  {:type :ram-test/actual-count/invalid-input
                   :tree tree})))
(deftest count-enforced
  "Check to see that all sizes are everywhere correct in the tree."
  (testing "Checking empty set..."
    (is (= (actual-count (empty-ram)) (size (empty-ram)) 0)))
  (testing "Checking inserted-in-order set..."
    (is (= (actual-count inserted-in-order) (size inserted-in-order))))
  (testing "Checking inserted-in-reverse-order set..."
    (is (= (actual-count inserted-in-reverse-order) (size inserted-in-reverse-order))))
  (testing "Checking inserted-in-wierd-order set..."
    (is (= (actual-count inserted-in-wierd-order) (size inserted-in-wierd-order))))
  (testing "Checking large-set set..."
    (is (= (actual-count large-set) (size large-set))))
  (testing "Checking large-reverse-set set..."
    (is (= (actual-count large-reverse-set) (size large-reverse-set))))
  (testing "Checking first-removed-tree set..."
    (is (= (actual-count first-removed-tree) (size first-removed-tree))))
  (testing "Checking second-removed-tree set..."
    (is (= (actual-count second-removed-tree) (size second-removed-tree))))
  (testing "Checking even-removed set..."
    (is (= (actual-count even-removed) (size even-removed)))))

(deftest test-ram-find
  "Test ram-find"
  (testing "Find a value in the set..."
    (is (= (ram-find inserted-in-order 1 compare) [1 1])))
  (testing "Don't find a value in the set..."
    (is (nil? (ram-find inserted-in-order 11 compare))))
  (testing "Don't find anything in the empty set..."
    (is (nil? (ram-find (empty-ram) nil compare)))))

(deftest get-by-rank-test
  "Find out if get-by-rank works."
  (testing "Checking rank on odd-numbered set..."
    (is (= (nth even-removed 0) 1))
    (is (= (nth even-removed 1) 3))
    (is (= (nth even-removed 2) 5))
    (is (= (nth even-removed 3) 7))
    (is (= (nth even-removed 4) 9)))
  (testing "Checking exception"
    (is (thrown-with-msg? clojure.lang.ExceptionInfo
                          #"Index went out of bounds."
                          (nth even-removed 5)))
    (is (thrown-with-msg? clojure.lang.ExceptionInfo
                          #"Index went out of bounds."
                          (nth even-removed -1))))
    (testing "Checking default value"
      (is (= (nth even-removed 80 nil) nil))
      (is (= (nth even-removed -1 nil) nil))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; inserted-in-reverse-order ;;
;; inserted-in-wierd-order   ;;
;; large-set                 ;;
;; large-reverse-set         ;;
;; first-removed-tree        ;;
;; second-removed-tree       ;;
;; even-removed              ;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
