package com.brettonw.bedrock.bag.expr;

import com.brettonw.bedrock.bag.BagObject;

import java.util.HashMap;
import java.util.Map;

public class Exprs {
    @FunctionalInterface
    interface ExprSupplier {
        Expr get (BagObject params);
    }

    private static final Map<String, ExprSupplier> exprSuppliers = new HashMap<> ();

    public static void register (String name, ExprSupplier exprSupplier) {
        exprSuppliers.put (name, exprSupplier);
    }

    public static Expr get (Object expr) {
        if (expr != null) {
            var bagObject = (BagObject) null;
            if (expr instanceof BagObject) {
                bagObject = (BagObject) expr;
            } else if (expr instanceof String) {
                bagObject = BagObject.open  (Expr.OPERATOR, Value.VALUE).put (Value.VALUE, expr);
            }
            if (bagObject != null) {
                var operator = bagObject.getString(Expr.OPERATOR);
                if (operator != null) {
                    var exprSupplier = exprSuppliers.get(operator);
                    if (exprSupplier != null) {
                        return exprSupplier.get(bagObject);
                    }
                }
            }
        }
        return null;
    }

    static {
        register (Equality.EQUALITY, Equality::new);
        register (Not.NOT, Not::new);
        register (Key.KEY, Key::new);
        register (Value.VALUE, Value::new);
        register (And.AND, And::new);
        register (Or.OR, Or::new);
    }

    public static BooleanExpr equality (String key, Object value) {
        return (BooleanExpr) get (Equality.bag (Key.bag (key), Value.bag (value)));
    }

    public static BooleanExpr inequality (String key, Object value) {
        return (BooleanExpr) get (Not.bag (Equality.bag (Key.bag (key), Value.bag (value))));
    }
}
