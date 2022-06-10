package org.yinwang.rubysonar.ast;

import org.yinwang.rubysonar._;


public enum Op {
    // numeral
    Add,
    Sub,
    Mul,
    Div,
    Mod,
    Pow,
    FloorDiv,

    // comparison
    Eq,
    Eqv,
    Equal,
    Lt,
    Gt,

    // bit
    BitAnd,
    BitOr,
    BitXor,
    In,
    LShift,
    RShift,
    Invert,

    // 