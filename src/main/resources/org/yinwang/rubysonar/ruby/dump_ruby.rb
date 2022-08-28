
require 'ripper'
require 'pp'
require 'json'
require 'optparse'


# --------------------- utils ---------------------
def banner(s)
  puts "\033[93m#{s}:\033[0m"
end


class AstSimplifier

  def initialize(filename)
    @filename = filename

    f = File.open(filename, 'rb')
    @src = f.read
    f.close

    detected_enc = detect_encoding(@src)
    if detected_enc
      begin
        @src.force_encoding(detected_enc)
      rescue
        @src.force_encoding('utf-8')
      end
    else
      # default to UTF-8
      @src.force_encoding('utf-8')
    end

    @src.encode('utf-8',
                {:undef => :replace,
                 :invalid => :replace,
                 :universal_newline => true}
    )

    @line_starts = [0]
    find_line_starts
    find_docs
  end


  def detect_encoding(s)
    # take first two lines
    header = s.match('^.*\n?.*\n?')
    if header and header[0]
      matched = header[0].match('^\s*#.*coding\s*[:=]\s*([\w\d\-]+)')
      if matched and matched[1]
        matched[1]
      end
    end
  end


  # initialize the @line_starts array
  # used to convert (line,col) location to (start,end)
  def find_line_starts
    lines = @src.split(/\n/)
    total = 0
    lines.each { |line|
      total += line.length + 1 # line and \n
      @line_starts.push(total)
    }
  end


  def find_docs
    @docs = {}
    lines = @src.split(/\n/)
    first_line = nil
    current_line = 0
    accum = []

    lines.each { |line|
      matched = line.match('^\s*#\s*(.*)')
      if matched
        accum.push(matched[1])
        if !first_line
          first_line = current_line
        end
      elsif !accum.empty?
        doc = {
            :type => :string,
            :id => accum.join('\n'),
        }
        @docs[current_line+1] = doc
        @docs[first_line-1] = doc
        accum.clear
        first_line = nil
      end

      current_line += 1
    }
  end


  def node_start(loc)
    line = loc[0]
    col = loc[1]
    @line_starts[line-1] + col
  end


  def ident_end(start_idx)
    if @src[start_idx] == '[' and @src[start_idx + 1] == ']'
      return start_idx + 2
    end
    idx = start_idx
    while idx < @src.length and @src[idx].match /[[:alpha:]0-9_@$\?!]/
      idx += 1
    end
    idx
  end


  def simplify
    tree = Ripper::SexpBuilder.new(@src).parse

    if $options[:debug]
      banner 'sexp'
      pp tree
    end

    simplified = convert(tree)
    simplified = find_locations(simplified)

    if $options[:debug]
      banner 'simplified'
      pp simplified
    end

    simplified
  end


  def find_locations(obj)
    def find1(obj)
      if obj.is_a?(Hash)
        #if obj[:type] == :binary and not obj[:left]
        #  puts "problem obj: #{obj.inspect}"
        #end
        ret = {}
        whole_start = nil
        whole_end = nil
        start_line = nil
        end_line = nil

        obj.each do |k, v|
          if k == :location
            start_idx = node_start(v)
            end_idx = ident_end(start_idx)
            ret[:start] = start_idx
            ret[:end] = end_idx
            ret[:start_line] = v[0]
            ret[:end_line] = v[1]
            whole_start = start_idx
            whole_end = end_idx
            start_line = v[0]
            end_line = v[1]
          else
            new_node, start_idx, end_idx, line_start, line_end = find1(v)
            ret[k] = new_node

            if start_idx && (!whole_start || whole_start > start_idx)
              whole_start = start_idx
              start_line = line_start
            end

            if end_idx && (!whole_end || whole_end < end_idx)
              whole_end = end_idx
              end_line = line_end
            end
          end
        end

        if whole_start
          # push whole_end to 'end' keyword
          if [:module, :class, :def, :lambda, :if, :begin, :while, :for]
              .include?(obj[:type]) and not obj[:mod]
            locator = whole_end
            while locator <= @src.length and
                not 'end'.eql? @src[locator .. locator + 'end'.length-1]
              locator += 1
            end
            if 'end'.eql? @src[locator .. locator + 'end'.length-1]
              whole_end = locator + 'end'.length
            end
          end

          ret[:start] = whole_start
          ret[:end] = whole_end
          ret[:start_line] = start_line
          ret[:end_line] = end_line

          # insert docstrings for node if any
          if [:module, :class, :def].include?(ret[:type])
            doc = @docs[start_line]
            if doc
              ret[:doc] = doc
            end
          end
        end
        return ret, whole_start, whole_end, start_line, end_line

      elsif obj.is_a?(Array)
        ret = []
        whole_start = nil
        whole_end = nil

        for v in obj
          new_node, start_idx, end_idx, line_start, line_end = find1(v)
          ret.push(new_node)
          if  start_idx && (!whole_start || whole_start > start_idx)
            whole_start = start_idx
            start_line = line_start
          end

          if end_idx && (!whole_end || whole_end < end_idx)
            whole_end = end_idx
            end_line = line_end
          end
        end

        return ret, whole_start, whole_end, start_line, end_line
      else
        return obj, nil, nil, nil, nil
      end
    end

    node, _, _, _, _ = find1(obj)
    node
  end


  # ------------------- conversion --------------------
  # convert and simplify ruby's "sexp" into a hash
  # exp -> hash
  def convert(exp)
    if exp == nil
      {}
    elsif exp == false
      {
          :type => :name,
          :id => 'false',
      }
    elsif exp == true
      {
          :type => :name,
          :id => 'true',
      }
    else
      case exp[0]
        when :program
          {
              :type => :program,
              :body => convert(exp[1]),
              :filename => @filename
          }
        when :module
          {
              :type => :module,
              :name => convert(exp[1]),
              :body => convert(exp[2]),
              :filename => @filename
          }
        when :@ident, :@op
          {
              :type => :name,
              :id => exp[1],
              :location => exp[2],
          }
        when :@gvar
          {
              :type => :gvar,
              :id => exp[1],
              :location => exp[2]
          }
        when :dyna_symbol
          # ignore dynamic symbols for now
          {
              :type => :name,
              :id => '#dyna_symbol'
          }
        when :symbol
          sym = convert(exp[1])
          sym[:type] = :symbol
          sym
        when :@cvar
          {
              :type => :cvar,
              :id => exp[1][2..-1],
              :location => exp[2]
          }
        when :@ivar
          {
              :type => :ivar,
              :id => exp[1][1..-1],
              :location => exp[2]
          }
        when :@const, :@kw, :@backtick
          #:@const and :@kw are just names
          {
              :type => :name,
              :id => exp[1],
              :location => exp[2]
          }
        when :@label
          {
              :type => :name,
              :id => exp[1][0..-2],
              :location => exp[2]
          }
        when :def
          {
              :type => :def,
              :name => convert(exp[1]),
              :params => convert(exp[2]),
              :body => convert(exp[3])
          }
        when :defs
          name = {
              :type => :attribute,
              :value => convert(exp[1]),
              :attr => convert(exp[3])
          }
          {
              :type => :def,
              :name => name,
              :params => convert(exp[4]),
              :body => convert(exp[5])
          }
        when :do_block
          {
              :type => :lambda,
              :params => convert(exp[1]),
              :body => convert(exp[2])
          }
        when :lambda
          {
              :type => :lambda,
              :params => convert(exp[1]),
              :body => convert(exp[2])
          }
        when :brace_block
          {
              :type => :lambda,
              :params => convert(exp[1]),
              :body => convert(exp[2])
          }
        when :params
          ret = {:type => :params}
          if exp[1]
            ret[:positional] = convert_array(exp[1])
          end
          if exp[2]
            # keyword arguments (converted into positionals and defaults)
            unless ret[:positional]
              ret[:positional] = []
            end
            exp[2].each { |x| ret[:positional].push(convert(x[0])) }
            ret[:defaults] = exp[2].map { |x| convert(x[1]) }
          end
          if exp[3] and exp[3] != 0
            ret[:rest] = convert(exp[3])
          end
          if exp[4]
            ret[:after_rest] = convert_array(exp[4])
          end
          if exp[6]
            ret[:rest_kw] = convert(exp[6])
          end
          if exp[7]
            ret[:blockarg] = convert(exp[7])
          end
          ret
        when :block_var
          params = convert(exp[1])
          if exp[2]
            params[:block_var] = convert_array(exp[2])
          end
          params
        when :class
          ret = {
              :type => :class,
              :static => false,
              :name => convert(exp[1]),
              :body => convert(exp[3]),
          }
          if exp[2]
            ret[:super] = convert(exp[2])
          end
          ret
        when :sclass
          {
              :type => :class,
              :static => true,
              :name => convert(exp[1]),
              :body => convert(exp[2]),
          }
        when :method_add_block
          call = convert(exp[1])
          if call[:args]
            call[:args][:blockarg] = convert(exp[2])
          else
            call[:args] = {
              :blockarg => convert(exp[2])
            }
          end
          call
        when :method_add_arg
          call = convert(exp[1])
          call[:args] = convert(exp[2])
          call
        when :vcall
          {
              :type => :call,
              :func => convert(exp[1])
          }
        when :command
          {
              :type => :call,
              :func => convert(exp[1]),
              :args => convert(exp[2])
          }
        when :command_call
          if exp[2] == :'.' or exp[2] == :'::'
            func = {
                :type => :attribute,
                :value => convert(exp[1]),
                :attr => convert(exp[3])
            }
          else
            func = convert(exp[1])
          end
          {
              :type => :call,
              :func => func,
              :args => convert(exp[4])
          }
        when :super, :zsuper