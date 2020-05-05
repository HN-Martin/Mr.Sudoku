(ns mrsudoku.engine
  (:use midje.sweet)
  (:require [mrsudoku.grid :as g]
            [clojure.set :as s] ))

(def ^:private sudoku-grid (var-get #'g/sudoku-grid))

(defn values
  "Return the set of values of a vector or grid `cells`."
  [cells]
  (loop [c cells, res #{}]
    (if (seq c) 
      (recur (rest c) (conj res (g/cell-value (first c))))
      (disj res nil))))



(fact
 (values (g/block sudoku-grid 1)) => #{5 3 6 9 8})

(fact
 (values (g/row sudoku-grid 1)) => #{5 3 7})

(fact
 (values (g/col sudoku-grid 1)) => #{5 6 8 4 7})

(fact
 (values (g/block sudoku-grid 8)) => #{4 1 9 8})

(fact
 (values (g/row sudoku-grid 8)) => #{4 1 9 5})

(fact
 (values (g/col sudoku-grid 8)) => #{6 8 7})

(defn values-except
  "Return the set of values of a vector of cells, except the `except`-th."
  [cells except]
  {:pre [(<= 1 except (count cells))]}
  (values (concat (subvec cells 0 (dec except)) (subvec cells except))))


(fact
 (values-except (g/block sudoku-grid 1) 1) => #{3 9 6 8})

(fact
 (values-except (g/block sudoku-grid 1) 4) => #{3 9 5 8})

(defn mk-conflict [kind cx cy value]
  {:status :conflict
   :kind kind
   :value value})

(defn merge-conflict-kind
  [kind1 kind2]
  (cond
    (and (set? kind1) (set? kind2)) (s/union kind1 kind2)
    (set? kind1) (conj kind1 kind2)
    (set? kind2) (conj kind2 kind1)
    (= kind1 kind2) kind1
    :else (hash-set kind1 kind2)))

(fact
 (merge-conflict-kind :row :row) => :row)

(fact
 (merge-conflict-kind :row :block) => #{:row :block})

(fact
 (merge-conflict-kind :row #{:row :block}) => #{:row, :block})

(fact
 (merge-conflict-kind #{:row :block} :block) => #{:row, :block})

(fact
 (merge-conflict-kind #{:row :block} #{:block :col}) => #{:row :block :col})


(defn merge-conflict [conflict1 conflict2]
  (assoc conflict1 :kind (merge-conflict-kind (:kind conflict1) (:kind conflict2))))

(defn unionORfirst [c1 c2]
  (cond
    (and (set? c1) (set? c2)) (s/union c1 c2)
    (set? c1) (conj c1 c2)
    (set? c2) (conj c2 c1)
    :else c1))

(defn merge-conflicts [& conflicts]
  (apply (partial merge-with merge-conflict) conflicts))

(defn update-conflicts
  [conflict-kind cx cy value conflicts]
  (if-let [conflict (get conflicts [cx, cy])]
    (assoc conflicts [cx, cy] (mk-conflict (merge-conflict-kind conflict-kind (:kind conflict))
                                           cx cy value))
    (assoc conflicts [cx, cy] (mk-conflict conflict-kind cx cy value))))

(defn conflict-value [values except cell]
  (when-let [value (g/cell-value cell)]
    (when (and (not= (:status cell) :init)
               (contains? (values-except values except) value))
      value)))

(defn row-conflicts
  "Returns a map of conflicts in a `row`."
  [row cy]
  (loop [r row ,res {}, vu {}, cx 1]
    (if (seq r)
     (let [ele (first r) elev (get ele :value -1) eles (get ele :status)]
       (cond
         (= r {}) res
         (= elev -1) (recur (rest r) res vu (inc cx))
         (not (<= 1 elev 9)) (recur (rest r) (assoc res (vector cx cy) {:status :conflict, :kind :row, :value elev}) vu (inc cx))
         (contains? vu elev) (cond
                               (not= eles :set) (recur (rest r) (assoc res (vector (get vu elev) cy) {:status :conflict, :kind :row, :value elev}) (assoc vu elev cx) (inc cx))
                               (not= :set (get (nth row (dec (get vu elev))) :status)) (recur (rest r) (assoc res (vector cx cy) {:status :conflict, :kind :row, :value elev}) vu (inc cx))
                               :else (recur (rest r) (assoc res (vector cx cy) {:status :conflict, :kind :row, :value elev} (vector (get vu elev) cy) {:status :conflict, :kind :row, :value elev}) vu (inc cx)))
         :else (recur (rest r) res (assoc vu elev cx) (inc cx))))
      res)))

(fact
 (row-conflicts (map #(g/mk-cell :set %) [1 2 3 4]) 1) => {})
      
(fact
 (row-conflicts (map #(g/mk-cell :set %) [1 2 3 1]) 1)
 => {[1 1] {:status :conflict, :kind :row, :value 1},
     [4 1] {:status :conflict, :kind :row, :value 1}})

(fact
 (row-conflicts [{:status :init, :value 8} {:status :empty} {:status :empty} {:status :empty} 
                 {:status :init, :value 6} {:status :set, :value 6} {:status :empty} {:status :empty} 
                 {:status :init, :value 3}] 4)
 => {[6 4] {:status :conflict, :kind :row, :value 6}})

(defn rows-conflicts [grid]
  (reduce merge-conflicts {}
          (map (fn [r] (row-conflicts (g/row grid r) r)) (range 1 10))))

(defn col-conflicts
  "Returns a map of conflicts in a `col`."
  [col cx]
  (loop [r col ,res {}, vu {}, cy 1]
    (if (seq r)
      (let [ele (first r) elev (get ele :value -1) eles (get ele :status)]
        (cond
          (= r {}) res
          (= elev -1) (recur (rest r) res vu (inc cy))
          (not (<= 1 elev 9)) (recur (rest r) (assoc res (vector cx cy) {:status :conflict, :kind :row, :value elev}) vu (inc cy))
          (contains? vu elev) (cond
                                (not= eles :set) (recur (rest r) (assoc res (vector cx (get vu elev)) {:status :conflict, :kind :row, :value elev}) (assoc vu elev cy) (inc cy))
                                (not= :set (get (nth col (dec (get vu elev))) :status)) (recur (rest r) (assoc res (vector cx cy) {:status :conflict, :kind :row, :value elev}) vu (inc cy))
                                :else (recur (rest r) (assoc res (vector cx cy) {:status :conflict, :kind :row, :value elev} (vector cx (get vu elev)) {:status :conflict, :kind :row, :value elev}) vu (inc cy)))
          :else (recur (rest r) res (assoc vu elev cy) (inc cy))))
      res)))

(fact
 (col-conflicts (map #(g/mk-cell :set %) [1 2 3 4]) 1) => {})

(fact
 (col-conflicts (map #(g/mk-cell :set %) [1 2 3 1]) 1)
 => {[1 1] {:status :conflict, :kind :row, :value 1}
     [1 4] {:status :conflict, :kind :row, :value 1}})

(fact
 (col-conflicts [{:status :init, :value 8} {:status :empty} {:status :empty} 
                 {:status :empty} {:status :init, :value 6} {:status :set, :value 6} 
                 {:status :empty} {:status :empty} {:status :init, :value 3}] 4)
 => {[4 6] {:status :conflict, :kind :row, :value 6}})

(defn cols-conflicts
  [grid] (reduce merge-conflicts {}
                 (map (fn [c] (col-conflicts (g/col grid c) c)) (range 1 10))))

(defn coord 
  "returns the coordinates of the x 'th cell of the b'th block"
  [cb lb x]
  (let [cx (rem (dec x) 3) lx (quot (dec x) 3)]
  (vector (+ 1 cx (* 3 cb)) (+ 1 lx (* 3 lb))))
  )

(defn block-conflicts
  [block b] 
    (let [cb (rem (dec b) 3) lb (quot (dec b) 3)]
     (loop [r block ,res {}, vu {}, i 1]
      (if (seq r)
        (let [ele (first r) elev (get ele :value -1) eles (get ele :status)]
          (cond
            (= r {}) res
            (= elev -1) (recur (rest r) res vu (inc i))
            (not (<= 1 elev 9)) (recur (rest r) (assoc res (coord cb lb i) {:status :conflict, :kind :row, :value elev}) vu (inc i))
            (contains? vu elev) (cond
                                  (not= eles :set) (recur (rest r) (assoc res (vector (coord cb lb (get vu elev))) {:status :conflict, :kind :row, :value elev}) (assoc vu elev i) (inc i))
                                  (not= :set (get (nth block (dec (get vu elev))) :status)) (recur (rest r) (assoc res (coord cb lb i) {:status :conflict, :kind :row, :value elev}) vu (inc i))
                                  :else (recur (rest r) (assoc res (coord cb lb i) {:status :conflict, :kind :row, :value elev} (coord cb lb (get vu elev)) {:status :conflict, :kind :row, :value elev}) vu (inc i)))
            :else (recur (rest r) res (assoc vu elev i) (inc i))))
        res))))

(fact
 (block-conflicts (map #(g/mk-cell :set %) [1 2 3 4]) 1) => {})

(fact
 (block-conflicts (map #(g/mk-cell :set %) [1 2 3 1]) 6)
 => {[7 5] {:status :conflict, :kind :row, :value 1}
     [7 4] {:status :conflict, :kind :row, :value 1}})

(fact
 (block-conflicts [{:status :init, :value 8} {:status :empty} {:status :empty} {:status :empty}
                 {:status :init, :value 6} {:status :set, :value 6} {:status :empty} {:status :empty}
                 {:status :init, :value 3}] 4)
 => {[3 5] {:status :conflict, :kind :row, :value 6}})

(defn blocks-conflicts
  [grid]
  (reduce merge-conflicts {}
          (map (fn [b] (block-conflicts (g/block grid b) b)) (range 1 10))))

(defn grid-conflicts
  "Compute all conflicts in the Sudoku grid."
  [grid]
  (merge-conflicts (rows-conflicts grid)
                   (cols-conflicts grid)
                   (blocks-conflicts grid)))
