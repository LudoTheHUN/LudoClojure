(ns LudoClojure.pp-datamaker
  (:require [clojure.string :as st])
  )

(import '(java.net URL)
        '(java.lang StringBuilder)
        '(java.io BufferedReader InputStreamReader))

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

 (def url_with_data "http://archive.ics.uci.edu/ml/machine-learning-databases/car/car.data")
 (def url_with_data "http://archive.ics.uci.edu/ml/machine-learning-databases/iris/iris.data")
 
 (def fetched_data (fetch-url url_with_data))

 (count fetched_data)
 (map (fn [x] (st/split x #",")) fetched_data)

(defn set_of_values [col split_data] (reduce conj #{} (map (fn [x] (nth x col)) split_data)) )

(defn list_set_of_values [fetched_data]
 (let  [split_data (map (fn [x] (st/split x #",")) fetched_data)
        width      (count (second split_data))
        split_data_f (filter (fn [x] (= (count x) width)) split_data)]
   (map (fn [x] (set_of_values x split_data_f)) (range width))
   ;(first(first split_data))
   ;(map (fn [x] (nth x 0)) split_data)
   ;split_data_f
   ))

(list_set_of_values fetched_data)

(defn detect_types  [list_of_distinct_values_set]
    (let [count_of_columns (count list_of_distinct_values_set)
          count_column_values      (fn [x]  (count (nth (list_set_of_values fetched_data) x)))
          count_column_values_nums (fn [x]  (count (filter true? (map (fn [x]
                                                                        (try
                                                                           (number? (read-string x))
                                                                           (catch Exception e false)
                                                                           ))
                                                        (nth (list_set_of_values fetched_data) x)))))
          ]
      [(map count_column_values       (range count_of_columns ))    ;(35 23 43 22 3)
      (map count_column_values_nums  (range count_of_columns))]     ;(35 23 43 22 0)
      
      ))
(count (list_set_of_values fetched_data))
(detect_types (list_set_of_values fetched_data))





  (nth (list_set_of_values fetched_data) 0)
  (count (filter true? (map (fn [x](number? (read-string x)))
              (first (list_set_of_values fetched_data)))))
  
 (reduce (fn [x y] (and x y)) (map (fn [x](number? (read-string x)))
              (first (list_set_of_values fetched_data)))
         )

 (def testdata (map (fn [x] (reduce str x)) fetched_data))

 (count fetched_data)

 (reduce str (first fetched_data))
 
 
 