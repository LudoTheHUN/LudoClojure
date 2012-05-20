(ns LudoClojure.test.spinopencl
  (:use [LudoClojure.spindle])
  (:use [LudoClojure.spinopencl])
  (:use [clojure.test])
  )

;Make an empty openCL spindle
(def opencl_spindle (make_spindle 100 10))
;Load in openCL kernels
(spindle_add_openCLsource! opencl_spindle (get_openCL_from_kernels opencl_kernels))
;Start the openCL spindle used in all subsequent tests
(start_spindle! opencl_spindle)

(def openCL_kernel_source (get_openCL_from_kernels opencl_kernels))

(keys @opencl_kernels)

(def a_float_array [0.1 0.2 3 3245334])
(def a_int_array [1 2 3 4 7])

(def float_array_buff  (make_buf opencl_spindle a_float_array :float32))
(def float_array_buff2 (make_buf opencl_spindle a_float_array :float32))
(def int_array_buff   (make_buf opencl_spindle a_int_array   :int32))

(deftest test_opencl_kernels_existance
   ;;Test the existance of inbuilt kernels
   (is (>  (count @opencl_kernels) 2))
   )


(deftest test_opencl_make_buf
   ;;Test the existance of inbuilt kernels
  (let [a_buf (make_buf opencl_spindle a_float_array :float32)]
   (is (= (class a_buf) calx.data.Buffer))
   (:buffer a_buf))
  )


(deftest test_opencl_read_buf
  ;;Testing read_buf
  (is (= (:elements float_array_buff) 4))
  (let [b_float_array (conj (vector-of :float) 0.2 0.4 0.6 0.8 1.0)
        b_float_buf (make_buf opencl_spindle b_float_array :float32)]
     (is (= (read_buf opencl_spindle b_float_buf) b_float_array)))
  (let [c_int_array (conj (vector-of :int) 1 7 8 9 11)
        c_int_buf (make_buf opencl_spindle c_int_array :int32)]
     (is (= (read_buf opencl_spindle c_int_buf) c_int_array)))
)

(deftest test_opencl_make_empty_buf
    (is (= (count (read_buf opencl_spindle (make_empty_buf opencl_spindle 10 :float32)))
           10))
    (is (= (count (read_buf opencl_spindle (make_empty_buf opencl_spindle 13 :int32)))
           13))
    (is (= (class (read_buf opencl_spindle (make_empty_buf opencl_spindle 111 :float32)))
           clojure.lang.PersistentVector))
      )

(deftest test_opencl_weave_kernel!
    (let [c_float_array (conj (vector-of :float) 4.2 4.4 44.6 444.8 5.0)
         local_float_buf (make_buf opencl_spindle c_float_array :float32)
         ]
     (is (= (read_buf opencl_spindle local_float_buf) c_float_array))
     (weave_kernel! opencl_spindle :addOneToFloat 5 
                    local_float_buf 
                    local_float_buf)
     (is (= (read_buf opencl_spindle local_float_buf) 
            (map (fn [x] (+ x (float 1.0))) c_float_array))))
)

;(weave_kernel! opencl_spindle :foo1 3 float_array_buff2 local_float_buf)


(deftest test_opencl_copy_buf_to_buf!
  (let [ c_float_array (conj (vector-of :float) 2.2 33.4 34.6 34.8 45.0)
         local_float_buf1 (make_empty_buf opencl_spindle 5 :float32)
         local_float_buf2 (make_buf opencl_spindle c_float_array :float32)
        ]
     (do (is (= (read_buf opencl_spindle local_float_buf2) c_float_array))
         ;(opencl_checkpoint opencl_spindle)
         (copy_buf_to_buf! opencl_spindle local_float_buf2 local_float_buf1)
         (is (= (read_buf opencl_spindle local_float_buf1)
                (read_buf opencl_spindle local_float_buf2))))
   )
   (let [ c_float_array (conj (vector-of :float) 2 33 64 34 45)
         local_float_buf1 (make_empty_buf opencl_spindle 5 :int32)
         local_float_buf2 (make_buf opencl_spindle c_float_array :int32)
        ]
     (do (is (= (read_buf opencl_spindle local_float_buf2) c_float_array))
         ;(opencl_checkpoint opencl_spindle)
         (copy_buf_to_buf! opencl_spindle local_float_buf2 local_float_buf1)
         (is (= (read_buf opencl_spindle local_float_buf1)
                (read_buf opencl_spindle local_float_buf2))))
   )
)





(quote

(weave_kernel! opencl_spindle :addOneToFloat 3 float_array_buff float_array_buff2)
(read_buf opencl_spindle float_array_buff)
(time (do (read_buf opencl_spindle float_array_buff2)
       (println (sindle_joblag opencl_spindle))))

(time (do
        (weave_kernel! opencl_spindle :addOneToFloat 3 float_array_buff2 float_array_buff2)
        (println (sindle_joblag opencl_spindle))
        (Thread/sleep 1)
        (println (sindle_joblag opencl_spindle))
      ))

(sindle_joblag opencl_spindle)

;;(weave! opencl_spindle #(enqueue-kernel :foo1 3 float_array_buff float_array_buff2))

;    (time (run-tests))





(let [foo (time (read_buf opencl_spindle (make_empty_buf opencl_spindle 1000000 :float32)))]
    (time (count foo)))

(defn make_random_float_array [size]
  "returns a deterministic random float array vector"
   (doall
   (let [r (java.util.Random. 12345)]
     (loop [i 0 outlist ()]
       (if (= i size)
         outlist
         (recur (inc i) (conj outlist (float(* (.nextInt r 1000) 0.001)))))))))

(let [foo (time (make_random_float_array 1000000))]
(time (count foo)))




(def foobuff (make_buf opencl_spindle [1.0 1.123 -1.0] :float32))
(enqueue-overwrite foobuff [0 2] [2.0 2.32 1.12])
(read_buf opencl_spindle foobuff)



)







