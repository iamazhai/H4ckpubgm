# ------------- class and instance methods with same name -----------------
class C
  class << self
    def foo
      "class method foo"
    end
  end

  def foo
    "instance 