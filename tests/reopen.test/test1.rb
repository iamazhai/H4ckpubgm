class A
  def foo
    puts 'this is foo'
  end
end

A.baz  # not there


class B
  class << self
    def bar
      puts 't