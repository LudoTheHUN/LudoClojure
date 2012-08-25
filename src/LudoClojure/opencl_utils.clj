(ns LudoClojure.opencl-utils
  ;(:use [LudoClojure.spindle])
  ;(:use [LudoClojure.utils])
  (:use [calx])
  )
;(use 'calx)


(println "loading opencl-utils")

;;Make the openCL environment here.  Make it just once, lets users add queues if need be.

(def opencl_env
  (let [my_devices (available-devices (platform))
        my_context (apply create-context (available-devices (platform)))
        my_queue (lg_create-queue (first my_devices) my_context )]
  (atom 
                  {:devices my_devices
                   :context  my_context
                   :queue    my_queue})))

(defn opencl_env_addQueue [opencl_env_map queuekey]
"Note that queues share the keyname space with other openCL env object, anyone adopting a queue as a library will have to be carefull not to trod on them"
;;TODO instead of trodding, why no prevent double creation? Maybe throw error so it's obvious no one should add a queue more then once?
  (swap! opencl_env_map 
         (fn [m] 
            (conj m {queuekey (lg_create-queue (first (:devices m)) (:context m) )}))))


;;test with (opencl_env_addQueue opencl_env :queue2)


(defn opencl_env_compileprogs [opencl_env_map openclprog]
   (swap! opencl_env_map 
         (fn [m] 
            (conj m {:progs (lg_compile-program (:devices m) openclprog (:context m))}))))


;;test with (opencl_env_compileprogs opencl_env (get_openCL_from_kernels opencl_kernels))

(defn buf_elements [buf]
     (:elements buf))

(defn is_buffer? [buf]
  (= calx.data.Buffer (class buf)))
  

;;Example kernels to be added
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
     :desc "a long string about what the kernel does"
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
     :desc "copies one Int array to another"
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

:timestwo {
     :desc "takes float x and puts x*2 into y"
     :postion 2
     :body "
__kernel void timestwo(
    __global float *x,
    __global float *y
    )
{
    int gid = get_global_id(0);
    y[gid] = x[gid] * 2.0;
}
"
}
:timestwoSlowly {
     :desc "takes float x and puts x*2 into y but with a big delay"
     :postion 2
     :body "
__kernel void timestwoSlowly(
    __global float *x,
    __global float *y
    )
{
    int gid = get_global_id(0);
    int iatom = 0; 
    float iterslowly = 0.0;
    for(iatom = 0; iatom < 2000000; iatom+=1 ) {
    iterslowly = iterslowly + 1.0;
}
    y[gid] = (x[gid] * 2.0) + iterslowly - 2000000;
}
"
}
})


(defn openCL_copy_buf_to_buf! [queue progs copy_type buf1 buf2]
 "Copy a buffer to another buffer, note both have to have the same frame"
(if (>= (buf_elements buf2) (buf_elements buf1))
    (cond (= copy_type :copyFloatXtoY)   ;for use with :float32-le frames
            (lg_enqueue-kernel queue progs
                            :copyFloatXtoY
                            (buf_elements buf1) buf1 buf2)
          (= copy_type :copyIntXtoY)     ; for use with :int32-le frames
            (lg_enqueue-kernel queue progs
                             :copyIntXtoY
                             (buf_elements buf1) buf1 buf2)
          :else
            (throw (Exception. (str "No valid copy_type provided"))))
    (throw (Exception. (str "Can not copy buf to buff. Unknown frame or bad sizes between ")))))


;;TODO add queue branching and merging helpers here.



