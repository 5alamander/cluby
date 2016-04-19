# Cluby -- Ruby Object Model in Clojure

A Clojure library designed to simulate the OO model of Ruby.

在使用clojure描述问题时，经常遇到一些需要反引用的场景，每当这个时候，就十分怀念OOP呢。

于是就写了这个包，目的在于在Clojure中模仿Ruby的对象模型的构建。

* 注: 仅用于在Clojure中构建对象模型，其他内容还是使用Clojure和Java的东西比较合适

## Latest version
![](https://clojars.org/org.clojars.sa1amander/cluby/latest-version.svg)

## Install
下载：在project.clj中添加依赖，如下
```js
:dependencies [org.clojars.sa1amander/cluby "0.1.0-SNAPSHOT"]
```

## Usage

### 方法调用
只能说尽量保持语法一致了
```ruby
#ruby
Object.new() 
MyClass.new(1)

#clojure
(OBJECT :new)
(MyClass :new 1)
```

### 创建对象
从OBJECT, 和MODULE, 对象类中新建

* 注：OBJECT, CLASS, MODULE, BASIC-OBJECT，这几个基本的类用大写
* (Object 与java的Object冲突了。。。)

```clojure
;; clojure
(def obj (OBJECT :allocate))
(def module (MODULE :allocate))
```

```ruby
# ruby
obj = Object.allocate
mod = Module.allocate
```

### 定义类实例方法
定义类，和类的实例方法instance-methods，

方法定义直接使用clojure内置function，等效于 (fn [ ] ... ) ，直接使用原生的重载方法

多了一对括号，方便解析，以后省掉吧，囧
* TODO: 定义新的defm宏
```clojure
;; clojure
(defclass MyClass
    (defm method1 ([] :m))
    (defm multimethod (     ;; 不同参数个数的重载方法
        ([a] :m1)
        ([] :m0))))

(def my-obj (MyClass :new))
(my-obj :method1)           ;=> :m
(my-obj :multimethod 1)     ;=> :m1
(my-obj :multimethod)       ;=> :m0
```
在不使用黑魔法的前提下，ruby没有原生的重载方法（[ruby黑魔法实现]()）

```ruby
# ruby
class MyClass
    def method1()
        :m
    end
  ## def multimethod        # 没有重载方法
end

myObj = MyClass.new
myObj.method1               #=> :m
```

---为什么叫黑魔法呢，因为接下来就会遇到问题

### 定义方法到类对象上（实例方法）

单件类（eigenclass，singleton_class）：叫法比较多，还是教单件类好听些
```clojure
;; clojure
(defm MyClass a ([]
    :MyClass-method))
(MyClass :a)            	;=> :MyClass-method
```

```ruby
# ruby
def MyClass.a()
    :MyClass-method
end
MyClass.a()		     		#=> :MyClass-method
```
### 子类可以使用超类的单件方法
这个特性怎么能少
```clojure
;; clojure
(defclass MySubClass (extends MyClass))
(MySubClass :a)            	;=> :MyClass-method
```

```ruby
# ruby
class MySubClass < MyClass
end
MySubClass.a()		     	#=> :MyClass-method
```

### 定义方法到对象上（单件方法）
```clojure
;; clojure
(def my-obj (MyClass :new))
(defm my-obj a ([]
    :my-obj-method))
(my-obj :a)            	    ;=> :my-obj-method
```

```ruby
# ruby
myObj = MyClass.new
def myObj.a()
    :my-obj-method
end
myObj.a()		     		#=> :my-obj-method
```
### 类的继承

在类定义中用(extends MyClass)表示继承[任意位置，如果存在多个则最后一个生效]

使用(self :super ....)调用超类中的方法，

非常遗憾的是，重载方法会被子类的新的方法一次性覆盖掉，与常理不符合

上文所述的ruby黑魔法也有这个问题，黑魔法的副作用啊o(╯□╰)o
* TODO: 使用<符号表示继承

```clojure
;; clojure
(defclass SubClass (extends MyClass)
	(defm method1 ([]
		[:sub, (self :super :method1)])))
```
```ruby
# ruby
class SubClass < MyClass
	def method1()
		[:sub, super.method1()]
	end
end
```
### Mixin
module会被添加到先祖链上
```clojure
;; clojure
(defmodule Amodule
	(defm a ([] :module-a)))
(defclass Empty
	(include Amodule))
```
```ruby
# ruby
module Amodule
	def a (); :module-a ; end
end
class Empty
	include Amodule
end
```
### 实例对象上添加module
module会被添加到实例的eigenclass上
```clojure
;; clojure
(my-obj :extend Amodule)
```
```ruby
# ruby
myObj.extend(Amodule)
```
## Awesome Features with Clojure
### Delegate
由于对象是fn包装而成的，所以一切有关函数的特性都适用，
使用partial函数的来模拟delegate特性（还是带柯里化的delegate）
```clojure
(defclass MyClass
	(defm initialize ([]
		(self :set! :t 1)))
	(defm a ([]
    	(self :get :t)))
	(defm b ([arg1 arg2]
    	(+ arg1 arg2))))

;; 新建对象
(def my-obj (MyClass :new))

;; 创建delegate
(def delegate0 (partial my-obj :a))
(def delegate1 (partial my-obj :b))
(def delegate2 (partial my-obj :b 10))

(delegate0)          ; => 1
(delegate1 4 5)      ; => 9
(delegate2 4)        ; => 14
```
### PersistenceMap
实例对象的变量键值对，使用原生map类型保存，可以直接设置
```clojure
(def obj1 (MyClass :new))
(set-states obj1 {:a 1, :b 2, :c 3}) ; 直接设置
(obj1 :get :a)  ; => 1
(obj1 :set! :a 2)
(obj1 :get :a)  ; => 2
```

...

## License

Copyright © 2016 FIXME

Distributed under the Eclipse Public License either version 1.0 or (at your option) any later version.
