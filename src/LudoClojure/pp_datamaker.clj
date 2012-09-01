(ns LudoClojure.pp-datamaker
  (:require [clojure.string :as st])
)

(import '(java.net URL)
        '(java.lang StringBuilder)
        '(java.io BufferedReader InputStreamReader File)
        '(java.lang Math))

(defn fetch-url
  "Return the web page as a string."
  [address]
  (let [url (URL. address)]
    (with-open [stream (. url (openStream))]
      (let [buf (BufferedReader. (InputStreamReader. stream))]
       ; (map vec (line-seq buf))))))
        ;(line-seq buf)))))
       ;(apply str (line-seq buf))))))
       (apply  vector  (line-seq buf))))))


(defn localise [url]
 (let [filename (.getName (File. url))
       local_filename  (str "docs/" filename)
         ]
   (if (.exists (File. local_filename))
      nil
      (spit local_filename (fetch-url url)))
   local_filename))


(defn- get_resource_dispatch
  [& params]
  (let [lead-param (first params)]
    (cond
     (empty? params) ::empty
     (nil? lead-param) ::nil
     (try (URL. lead-param) (catch Exception e false)) :URL
     (try (File. lead-param) (catch Exception e false)) :File
     true :unknown)))

(defmulti get_resource get_resource_dispatch)

(defmethod get_resource :URL
  [url]
  (fetch-url url))

(defmethod get_resource :File
  [filename]
  (read-string (slurp filename)))

(def url "http://archive.ics.uci.edu/ml/machine-learning-databases/iris/iris.data")
(def url "http://archive.ics.uci.edu/ml/machine-learning-databases/car/car.data")

(get_resource (localise url))

(def fetched_data (get_resource (localise url)))

;;;;; Now read into data to asses it's data types
;;Support nummerics and classes

(count fetched_data)
(map (fn [x] (st/split x #",")) fetched_data)

(defn set_of_values [col split_data]
  "Load each 'column' of the data into a set, and thus get the distinct values seen"
   (reduce conj #{} (map (fn [x] (nth x col)) split_data)) )

(defn list_set_of_values [fetched_data]
  "List out the distinct set of values in each column"
 (let  [split_data (map (fn [x] (st/split x #",")) fetched_data)
        width      (count (second split_data))
        split_data_f (filter (fn [x] (= (count x) width)) split_data)]
   (map (fn [x] (set_of_values x split_data_f)) (range width))
   ))

(list_set_of_values fetched_data)


(defn numeric_column? [count_distinct_items count_distinct_numeric_items]
    (cond 
      (= count_distinct_numeric_items 0) :string
      (= (- count_distinct_items count_distinct_numeric_items) 1) :numeric_with_defaults
      (= count_distinct_items count_distinct_numeric_items) :numeric
      :else :unknown))



(defn detect_types  [list_of_distinct_values_set]
  "get overview of type counts in each column"
    (let [count_of_columns (count list_of_distinct_values_set)
          count_column_values      (fn [x]  (count (nth (list_set_of_values fetched_data) x)))
          count_column_values_nums (fn [x]  (count (filter true? (map (fn [x]
                                                                        (try
                                                                           (number? (read-string x))
                                                                           (catch Exception e false)
                                                                           ))
                                                        (nth (list_set_of_values fetched_data) x)))))
          count_distinct_items         (map count_column_values        (range count_of_columns))
          count_distinct_numeric_items (map count_column_values_nums   (range count_of_columns))
          column_data_types            (map numeric_column? count_distinct_items count_distinct_numeric_items)
          
          
          ]
      {:count_distinct_items count_distinct_items
       :count_distinct_numeric_items count_distinct_numeric_items
       :column_data_types column_data_types
       :array_of_distinct_items (map vec list_of_distinct_values_set)}
      ))

(defn bitarize [n bitmapsize]
  "create a bit array from an int representing it"
  (vec (map (fn [x] (if x 1.0 0.0)) (reverse (reduce (fn [state x] (conj state (bit-test n (count state)))) [] (range bitmapsize))))))
(time (bitarize  501 10))

(defn power [x]
 (reduce * (map (fn[_] 2) (range x))))
(power_of_2 5)

(defn enoughtbits [y] 
  (loop [iter 1]
    (if  (> (power_of_2 iter) y)
      iter
      (recur (inc iter)))))

(enoughtbits 456565345554545645)

(defn makebitz [x max_size]
(time (bitarize  x (enoughtbits max_size))))
(makebitz 2 23)



(defn powers_up
    ([] (powers 1))
    ([n] (lazy-seq (cons n (positive-numbers (reduce * (map (fn[_] 2) (range x))))))))


(list_set_of_values fetched_data)

(vec (first (list_set_of_values fetched_data)))

;given this pair, get a biterize
(first (map (fn [x] (st/split x #",")) fetched_data))
(first (first (map (fn [x] (st/split x #",")) fetched_data)))


(count (list_set_of_values fetched_data))


(first (map (fn [x] (st/split x #",")) fetched_data))



(find [:a :b :c :d] 2)



(def myset #{"Iris-virginica" "Iris-versicolor" "Iris-setosa"})
(disj #{"Iris-virginica" "Iris-versicolor" "Iris-setosa"} "Iris-setosa")
(reduce (fn[state x] (conj state [x    (count state)   ]))
     {} #{"Iris-virginica" "Iris-versicolor" "Iris-setosa"})
(defn bitarize [n bitmapsize]
  "create a bit array from an int representing it's number"
  (vec (map (fn [x] (if x 1.0 0.0)) (reverse (reduce (fn [state x] (conj state (bit-test n (count state)))) [] (range bitmapsize))))))
(time (bitarize  501 10))

(reduce * (map (fn [_] 2) (range 6)))







