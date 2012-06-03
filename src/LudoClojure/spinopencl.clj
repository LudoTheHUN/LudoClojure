(ns LudoClojure.spinopencl
  (:use [LudoClojure.spindle])
  )
(use 'calx)

;(println "hello from spin-opencl")

(def opencl_kernels 
^{:doc "
Atom holding map of kernel names to their specs. This atom will be global to all 
users, constituting a library of kernels.
kernels are stored in a map to kernelnames to maps of kernel specs,
one of the specs is a position of the kernel in the resultant kernel string
that should be assumed before compiling. Framework kernels come
first, then 'user kernels'. Order should matter as long as no support functions
are defined. User could choose to bootstrap a spindle with only some subset of
kernels and loose guarantee of functionality."}
(atom {}))

(defn add_kernel_specs [kernels_map kernel_specsmap]
^{:doc "Adds kernel specs to a kernel spec map"}
   (swap! kernels_map (fn [kernels_map] (conj kernels_map kernel_specsmap))))

(defn get_openCL_from_kernels [a_kernels_map]
^{:doc "gets openCL kernel source ordered as per the position specification"}
  (reduce str 
      (map :body 
         (sort-by :postion 
            (vals @a_kernels_map)))))


;;To add a kernel then do:
(add_kernel_specs opencl_kernels
;initial sets of most basic kernels
{
:testaddedkernel {
     :desc "a long string about that the kernel does"
     :postion 3
     :body "
__kernel void testaddedkernel(
    __global float *x,
    __global float *y
    )
{
    int gid = get_global_id(0);
    y[gid] = x[gid] - 1.0;
}
"}})

(add_kernel_specs opencl_kernels
{
:copyFloatXtoY {
     :desc "copies one float array to another"
     :postion 1
     :body "
__kernel void copyFloatXtoY(
    __global float *x,
    __global float *y
    )
{
    int gid = get_global_id(0);
    y[gid] = x[gid];
}
  "
}
:copyIntXtoY {
     :desc "copies one float array to another"
     :postion 1
     :body "
__kernel void copyIntXtoY(
    __global int *x,
    __global int *y
    )
{
    int gid = get_global_id(0);
    y[gid] = x[gid];
}
  "
}
:addOneToFloat {
     :desc "takes float x and puts x+1 into y"
     :postion 2
     :body "
__kernel void addOneToFloat(
    __global float *x,
    __global float *y
    )
{
    int gid = get_global_id(0);
    y[gid] = x[gid] + 1.0;
}
"
}
})

(defn valid_frame [frame]
  (or (= frame :float32-le) (= frame :int32-le)))



(defn opencl_checkpoint [spindle]
      (weave! spindle (fn [] (enqueue-barrier)(finish))))


(defn make_buf [spindle float_array frame]
^{:doc "Makes a buffer that lives on a spindle 
focus is on :float32 and :int32 glos types only"}
  (if (valid_frame frame)
   (weave! spindle #(conj (wrap float_array frame) [:frame frame]))
   :bad_frame__make_buf
))

(defn make_empty_buf [spindle number_of_elements frame]
^{:doc "Makes a empty buffer, filled with zeros, of the given size, 1 based "}
  (if (valid_frame frame)
    (weave! spindle 
         (fn [] 
           (conj (create-buffer number_of_elements frame) [:frame frame])))
    :bad_frame__make_empty_buf
           ))

(defn read_buf [spindle buf]
    ;;reads a buffer that lives on a spindle, emits an array of the buffer data.
    (weave! spindle (fn [] @(enqueue-read buf))))

(defn buf_elements [buf]
     (:elements buf))


(defmacro weave_kernel! [spindle kernel_keyword globalsize & bufs]
^{:doc "Side effects only, weaves away the a kernel on the given spindle"}
   `(weave_away! ~spindle #(enqueue-kernel ~kernel_keyword ~globalsize ~@bufs)))

;;TODO add a macro? to weave many kernels on a single spin, look out for needing to use (enqueue-barrier) so openCL queue doesn't shoot us in the foot

(defn copy_buf_to_buf! [spindle buf1 buf2]
  (if (and (= (:frame buf1) (:frame buf2))
           (>= (buf_elements buf2) (buf_elements buf1)))
    (cond 
      (= (:frame buf1) :float32)
         (weave_kernel! spindle :copyFloatXtoY (buf_elements buf1) buf1 buf2)
      (= (:frame buf1) :int32)
         (weave_kernel! spindle :copyIntXtoY (buf_elements buf1) buf1 buf2)
      :else 
         :bad_frame__copy_buf_to_buf2)
    :framesnotthesame_copy_buf_to_buf   ;;TODO consider an exeception instead
    ))






(quote testing-enqueue-overwrite
(def opencl_spindle (make_spindle 100 1))
(spindle_add_openCLsource! opencl_spindle (get_openCL_from_kernels opencl_kernels))
(start_spindle! opencl_spindle)
(def foobuff (make_buf opencl_spindle [1.0 1.123 -1.0] :float32))
(def foobuff2 (make_buf opencl_spindle [9.0 9.123 -9.0] :float32))
(to-buffer [2.0 2.32 1.12] :float32)


(defn weave_buf_overwrite! [spindle buf cloj_array frame]
    ;;Not this minus is WHACK, litle edian??!     ;;signature in calx;   ;[destination [lower upper] source]
    ;;This should be much faster then creating a whole new buffer as swap!ing it in. However, state managment is an a big concern... as is with kernel executions
    (weave_away! spindle (fn [] (enqueue-overwrite buf [-3 0] (to-buffer cloj_array frame)))))  ;;Not this minus is WHACK, litle edian??!

(defn weave_buf_copy! [spindle buf sourcebuf]
   ;;Copies openCL kernels to other openCL kernels   ;;signature in calx; [destination destination-offset source [lower upper]]
   ;;TODO find out how offsers work
    (weave_away! spindle (fn [] (enqueue-copy buf 0 sourcebuf [-2 0]  ))))

(enqueue-overwrite foobuff [0 2] (to-buffer [2.0 2.32 1.12] :float32))    ;[destination [lower upper] source]
(enqueue-copy foobuff 0 (to-buffer [2.0 2.32 1.12] :float32) [-2 0])         ; [destination destination-offset source [lower upper]]

;;speed tests
(time (dorun 50 (repeatedly (fn [] 
(let [largearray (apply vector (take 100000 (repeat 1.0)))
      foo (to-buffer largearray :float32)
      unfooed (from-buffer foo :float32)
      foobuffun (make_buf opencl_spindle unfooed :float32)
      unbuffed (read_buf opencl_spindle foobuffun)]
  (count unbuffed)    )))))

(time (def largearray (apply vector (take 1000040 (repeat 1.0)))))
(time (def foo (to-buffer largearray :float32)))
(time (def unfooed (from-buffer foo :float32)))
(time (def foobuffun (make_buf opencl_spindle unfooed :float32)))
(time (def unbuffed (read_buf opencl_spindle foobuffun)))

(read_buf opencl_spindle foobuff)

(time (to-buffer [2.0 2.32 1.12] :float32)) (time (weave_buf_overwrite! opencl_spindle foobuff [5.2 6.2 7.2] :float32))
(time (read_buf opencl_spindle foobuff))
(time (do (weave_buf_overwrite! opencl_spindle foobuff [5.2 6.2 7.2] :float32)
        (weave_buf_overwrite! opencl_spindle foobuff [5.2 6.2 7.2] :float32)
        (weave_buf_overwrite! opencl_spindle foobuff [5.2 6.2 7.2] :float32)
        (weave_buf_overwrite! opencl_spindle foobuff [5.2 6.2 7.2] :float32)
        (weave_buf_overwrite! opencl_spindle foobuff [5.2 6.2 7.2] :float32)
        (weave_buf_overwrite! opencl_spindle foobuff [5.2 6.2 7.2] :float32)
        (weave_buf_overwrite! opencl_spindle foobuff [5.2 6.2 44.2] :float32)
          (read_buf opencl_spindle foobuff)
      ))


(time (read_buf opencl_spindle foobuff))

(time (weave_buf_copy! opencl_spindle foobuff foobuff2))
(read_buf opencl_spindle foobuff)
(read_buf opencl_spindle foobuff2)


)




(quote testing work without calx macros
     ;; built it into calx1.4
(platform)
(available-devices (platform))

(context)
(create-context (available-devices (platform)) )

(defn create-context
  "Creates a context which uses the specified devices.  Using more than one device is not recommended."
  [& devices]
  {:context (.createContext ^CLPlatform (platform) nil (into-array devices))
   :cache (ref [])})

     (.createContext ^CLPlatform (platform) nil (into-array (available-devices (platform))))  
(into-array (available-devices (platform)))

       
       )


;TODO look at using defmulti + defmethod
;TODO hide away type of buffer?
