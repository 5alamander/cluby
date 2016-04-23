(in-ns 'cluby.core)

(set-instance-methods
 OBJECT {
         :extend (fn extend [module]    ; extend object with module
                   (swap! ((meta (get-object-eigenclass self)) :state)
                          update :modules conj module))
         :clone (fn clone []      ; allocate form this class and clone the state
                  (let [new-obj (*self-class* :allocate)]
                    (reset! ((meta new-obj) :state) @*self-state*)
                    new-obj))
         })
