(ns cluby.core)
;;; simulate OOP for ruby

(declare OBJECT CLASS MODULE class-find-method
         object-default-state class-default-state get-object-state)

(def ^:dynamic self nil)                ; bind self to thiz

(def ^:dynamic *current-class* nil)

(def ^:dynamic *self-class* nil)

(def ^:dynamic *self-state* nil)

(def ^:dynamic *self-variables* nil)

(def ^:dynamic *class-in-built* nil)    ; use when building class

(defn module-find-method [modules method-name]
  (loop [[m & rest-m] modules]
    (when m (if-let [method (method-name (m :ins-methods))]
              method (recur rest-m)))))

;;; find method in instance-methods, return [method, witch-class]
(defn class-find-method [klass method-name]
  (or (if-let [method ((klass :ins-methods) method-name)]
        [method klass])                 ; find method in current class
      (if-let [method (module-find-method (get-object-state klass :modules) method-name)]
        [method klass])                 ; find method in modules
      (if-let [super-class (klass :superclass)]
        (recur super-class method-name)))) ; find method in superclass

;;; send message to obj, with the klass (current class, or super class)
(defn- object-send-message [klass dstate command args]
  (let [[method the-klass]
        (or (class-find-method klass command) ; return the [method evn-class]
            (class-find-method klass :method-missing))]
    (if-not (fn? method)                ; do not respond to this message
      (throw (RuntimeException.         ; throw with the object of this message
              (str "Unable to respond to " command "\n" (meta (:self dstate))))))
    (binding [self (:self dstate), *current-class* the-klass]
      (apply method args))))

(defn- object-send-message-try [klass dstate command args]
  (let [[method the-klass] (class-find-method klass command)]
    (when method
      (binding [self (:self dstate), *current-class* the-klass]
        (apply method args)))))

(defn object-new [klass variables-map state-map]
  ;; set instance variables when create
  (let [variables (atom (if (map? variables-map) variables-map {}))
        state (atom (assoc state-map :class klass))
        obj
        (with-meta
          (fn [command & args]
            (condp = command
              :set! (let [[k v] args]
                      (swap! variables assoc k v) v) ; set! function returns nil
              :get (let [[key] args]
                     (key @variables))
              :get-state (let [[key] args]
                           (key @state))
              :ins-methods (:ins-methods @state)
              :super (let [[command & args] args ; get the superclass
                           klass (*current-class* :superclass)]
                       (object-send-message klass @state command args))
              (binding [*self-class* klass ; bind evn
                        *self-state* state, *self-variables* variables
                        *current-class* (or (:eigenclass @state) (:class @state))]
                (object-send-message *current-class* @state command args))))
          ;; set variables and state to meta data
          {:type :cluby-object
           :variables variables
           :state state})]
    ;; binding obj to self when sending message, this is ugly
    (swap! state assoc :self obj) obj))

(defn set-state [klass key value] ; set value to state
  (swap! ((meta klass) :state) assoc key value))

(defn set-states [klass states]
  (swap! ((meta klass) :state) merge states))

(defn set-instance-method [klass key f]
  {:pre [klass (fn? klass)]}
  ;; set method in class object
  (swap! ((meta klass) :state) update :ins-methods assoc key f))

(defn set-instance-methods [klass methods]
  (swap! ((meta klass) :state) update :ins-methods merge methods))

(defn get-object-state [obj key]
  (@((meta obj) :state) key))

(defn get-object-states [obj]
  @((meta obj) :state))

;;; --- eigenclass for object ---

(defn eigenclass-create [eigen-name eigen-superclass]
  {:pre [(string? eigen-name)]}
  (let [eigen (object-new CLASS nil object-default-state)]
    (set-states eigen {:name eigen-name :symbol (symbol eigen-name)
                       :superclass eigen-superclass})
    eigen))

;;; create object eigenclass by the object' class
(defn create-object-eigenclass [klass]  ; create an eigenclass of the klass
  (let [klass-state ((meta klass) :state)
        eigenclass (object-new CLASS nil object-default-state)
        eigen-name (str "#" (@klass-state :name))]
    (eigenclass-create eigen-name klass)))

(defn get-object-eigenclass [obj]
  ;; get or create eigenclass of a object
  (let [state ((meta obj) :state)
        eigenclass (@state :eigenclass)]
    (if eigenclass eigenclass
        ;; else create an eigenclass of this object
        (let [eigenclass (create-object-eigenclass (@state :class))]
          (swap! state assoc :eigenclass eigenclass)
          eigenclass))))

(defn singleton-method-add [obj key f]
  (let [eigenclass (get-object-eigenclass obj)]
    ;; ;TODO: invoke singleton-method-ad
    (swap! ((meta eigenclass) :state) update :ins-methods assoc key f)))

(defn singletion-method-remove [obj key]
  ;; ;TODO:
  )

(defn singleton-method-undefine [obj key]
  ;; ;TODO:
  )

;;; --- class functions ----

(defn class-allocate-obj [klass]
  (let [obj (object-new klass nil object-default-state)]
    (set-state obj :class-symbol (get-object-state klass :class-symbol))
    obj))

;;; set superclass of class-object and generate eigenclass
(defn class-set-superclass [klass superclass]
  (set-state klass :superclass superclass) ; set superclass then set eigen superclass
  (if-let [supereigen (get-object-state superclass :eigenclass)]
    (let [eigen-name (str "#" (get-object-state klass :name))]
      (set-state klass :eigenclass (eigenclass-create eigen-name supereigen)))
    (throw (RuntimeException.
            (str "create class, the superclass without eigenclass")))))

(defn class-new [symb superclass]
  ;; create an object as klass-object
  (let [klass-obj (object-new CLASS nil object-default-state)]
    ;; set symbols of this klass
    (set-states klass-obj {:name (str symb) :symbol symb})
    ;; set superclass and generate eigenclass
    (class-set-superclass klass-obj (or superclass OBJECT))
    klass-obj))

(defn module-new [symb]
  (let [module-obj (object-new MODULE nil object-default-state)]
    (set-states module-obj {:name (str symb) :symbol symb :superclass OBJECT})
    module-obj))

(def object-default-state
  {:class nil                           ; current object class
   :eigenclass nil
   :superclass nil                      ; for class object
   :ins-methods {}                      ; for class object
   :modules '()                         ; for class object
   })

(def class-default-state
  (merge object-default-state
         {:class CLASS
          :ins-methods
          {:allocate (fn allocate [] (class-allocate-obj self))
           :new (fn new [& args]
                  (let [the-obj (class-allocate-obj self)]
                    (object-send-message-try self (get-object-states the-obj)
                                             :initialize args)
                    the-obj))
           :superclass (fn superclass [] (self :get-state :superclass))}}))

(do ;; define class objects

  ;; def CLASS
  (def CLASS (object-new nil nil class-default-state))
  (set-states CLASS {:class CLASS, :name "CLASS"
                     :symbol 'CLASS})
  (set-instance-methods
   CLASS {:name (fn name [] (self :get-state :name))
          :to-s (fn to-s [] (str "<class:" (self :name) ">"))})

  ;; def OBJECT, which :superclass is nil
  (def BASIC-OBJECT (object-new CLASS nil object-default-state))
  (set-states BASIC-OBJECT {:name "BASIC-OBJECT" :symbol 'BASIC-OBJECT
                            :eigenclass (eigenclass-create "#BASIC-OBJECT" CLASS)})
  (def OBJECT (class-new 'OBJECT BASIC-OBJECT))
  (set-instance-methods
   OBJECT {:class (fn class [] (self :get-state :class))
           :to-s (fn to-s [] (str "<" ((self :class) :name) ":" self ">"))})

  ;; def MODULE
  (def MODULE (class-new 'MODULE OBJECT))
  (set-instance-methods
   MODULE {})

  ;; (set-state CLASS :superclass MODULE)
  (class-set-superclass CLASS MODULE)
  )

(defmacro defm "
  ;TODO: to simplify the method definition
  (defm OBJECT method ([] (...)))         ; '[] index is 2, keyword is 1
  (defm obj method ([] (...)))
  (defm obj method1 (([] (...)) ([a] (...))))
  (defclass NewObject
    (defm method ([] (...)))              ; '[] index is 1, keyword is 0
    (defm method1 (([] (...)) ([a] (...))))
  )
  "
  ([method fnbody]
   `(set-instance-method
     *class-in-built* ~(keyword method) ~(conj fnbody method 'fn)))
  ([target method fnbody]
   `(singleton-method-add
     ~target ~(keyword method) ~(conj fnbody method 'fn))))

(defmacro extends "
  (defclass NewClass
    (extends BaseClass)
    (defm method ([] (...)))
  )
  " [base-class]                        ; only works for class object
  `(class-set-superclass *class-in-built* @(resolve '~base-class)))

(defmacro include
  [module-obj]
  `(swap! ((meta *class-in-built*) :state)
          update :modules conj @(resolve '~module-obj)))

(defmacro defclass [class-name & specs]
  (if-let [the-class-ref (resolve class-name)]
    ;; get the symbol
    `(let [the-class# (deref ~the-class-ref)]
       (binding [*class-in-built* the-class#]
         ~@specs
         the-class#))
    ;; create a new class
    `(let [the-class-ref# (def ~class-name (class-new '~class-name OBJECT))
           the-class# (deref the-class-ref#)]
       (binding [*class-in-built* the-class#]
         ~@specs
         the-class#))))

(defmacro defmodule [module-name & specs]
  (if-let [the-module-ref (resolve module-name)]
    `(let [the-module# (deref ~the-module-ref)]
       (binding [*class-in-built* the-module#]
         ~@specs
         the-module#))
    `(let [the-module-ref# (def ~module-name (module-new '~module-name))
           the-module# (deref the-module-ref#)]
       (binding [*class-in-built* the-module#]
         ~@specs
         the-module#))))

;; (defmacro defclass
;;   [class-name & specs]
;;   (let [parent-class (parent-class-spec specs)
;;         fns (or (method-specs specs) {})]
;;     `(def ~class-name (new-class '~class-name #'~parent-class ~fns))))


;TODO: extend module in object

;;; load helper files
(load "core_object")
(load "core_class")
(load "core_module")
