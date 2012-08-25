(ns LudoClojure.test.opencl-utils
  (:use [LudoClojure.opencl-utils])
  (:use [calx])
  (:use [clojure.test]))


(opencl_env_compileprogs opencl_env (get_openCL_from_kernels opencl_kernels))

(deftest test_queues_and_events
  (let [data1 [0.1 0.2 0.3 0.4]
      buf1 (lg_wrap (:context @opencl_env) data1 :float32-le)
      data2 [0.2 0.2 0.2 0.2]
      buf2 (lg_wrap (:context @opencl_env) data2 :float32-le)
      data3 [0.3 0.3 0.3 0.3]
      buf3 (lg_wrap (:context @opencl_env) data3 :float32-le)
      extra_queue (opencl_env_addQueue opencl_env :myqu2)
      fast_event    (lg_enqueue-kernel (@opencl_env :queue) (@opencl_env :progs) :copyFloatXtoY  4 buf1 buf2)
      slow_event    (lg_enqueue-kernel (@opencl_env :myqu2) (@opencl_env :progs) :timestwoSlowly 4 buf1 buf3)
      ]
          
          (is (= (status fast_event) :complete))
          (is (= (status slow_event) :enqueued))
          (is (= @(lg_enqueue-read buf2 (@opencl_env :queue)) (map float data1)))
          (is (= (status fast_event) :complete))
          (is (or (= (status slow_event) :enqueued)))
          @(lg_enqueue-read buf3 (@opencl_env :myqu2))
          (is (= (status fast_event) :complete))
          (is (= (status slow_event) :complete))
)
)


(deftest test_queues_and_events2
  (let [data1 [0.1 0.2 0.3 0.4]
      buf1 (lg_wrap (:context @opencl_env) data1 :float32-le)
      data2 [0.2 0.2 0.2 0.2]
      buf2 (lg_wrap (:context @opencl_env) data2 :float32-le)
      data3 [0.3 0.3 0.3 0.3]
      buf3 (lg_wrap (:context @opencl_env) data3 :float32-le)
      extra_queue (opencl_env_addQueue opencl_env :myqu2)
      fast_event    (lg_enqueue-kernel (@opencl_env :queue) (@opencl_env :progs) :copyFloatXtoY  4 buf1 buf2)
      slow_event    (lg_enqueue-kernel (@opencl_env :myqu2) (@opencl_env :progs) :timestwoSlowly 4 buf1 buf3)
      ;;slow_event    (lg_enqueue-wait-for (@opencl_env :myqu2) fast_event)
      ]
          (is (= (status fast_event) :complete))
          (is (= (status slow_event) :enqueued))
          ;;(lg_enqueue-wait-for (@opencl_env :queue) slow_event)    ;;Force an error here
          (is (= @(lg_enqueue-read buf2 (@opencl_env :queue)) (map float data1)))
          (is (= (status fast_event) :complete))
          (is (or (= (status slow_event) :enqueued)))
          
          (is (= @(lg_enqueue-read buf3 (@opencl_env :myqu2)) (map float [0.25 0.375 0.625 0.75])))
          (is (= (status fast_event) :complete))
          (is (= (status slow_event) :complete))
           (is (= @(lg_enqueue-read buf3 (@opencl_env :myqu2)) (map float [0.25 0.375 0.625 0.75])))))

;;   (test_queues_and_events2)
 





