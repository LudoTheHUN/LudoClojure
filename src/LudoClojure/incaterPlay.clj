

(use 'incanter.core)
(def foo (dataset ["x1" "x2" "x3"] 
         [[1 2 3] 
          [4 5 6] 
          [7 8 9]]))



(set! *warn-on-reflection* true)
(use 'incanter.stats)
(def ^"[D" x (double-array (sample-normal 1e7)))
(def ^"[F" x (double-array (sample-normal 1e2)))


;;http://stackoverflow.com/questions/3814048/fast-vector-math-in-clojure-incanter
;(float-array size init-val-or-seq)

;(float-array size init-val-or-seq)

(for [i (range 10)] (float (rand)))

(def afloat_array (float-array 10 (for [i (range 10)] (float (rand)))))
(float? afloat_array)
(float? (nth afloat_array 1))

(set! *warn-on-reflection* true)


(def afloat_array (float-array 10000000 (for [i (range 10000000)] (float (rand)))))
(def a_vec (vec afloat_array))
(a_vec 999999)
(a_vec 1000000)
(a_vec 9999999)
(count a_vec)


;;;This is lots more compact
(set! *warn-on-reflection* true)

(def ^floats a_float-array (float-array 10000000 
         (for [i (range 10000000)] (float (rand)))))

(def ^floats a_vec_float-array (vec (float-array 10000000 
         (for [i (range 10000000)] (float (rand))))))

(def a_vec_fat (vec 
         (for [i (range 10000000)] (float (rand)))))

;to incanter...
(to-dataset a_float-array)
		 

