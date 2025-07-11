package com.cometkaizo.monarch;

import com.cometkaizo.analysis.AnalysisContext;
import com.cometkaizo.bytecode.Chunk;
import com.cometkaizo.bytecode.AssembleContext;
import com.cometkaizo.monarch.structure.*;
import com.cometkaizo.monarch.structure.resource.Type;
import com.cometkaizo.parser.ParseContext;
import com.cometkaizo.parser.Structure;
import com.cometkaizo.util.CharIterator;
import com.cometkaizo.util.DiagnosticList;
import com.cometkaizo.util.StringUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Map;
import java.util.Optional;

public class Compiler {
    public static final String SOURCE_EXT = "mnrk", BYTECODE_EXT = "mnrc";
    private CompilationUnit.Parser unitStx;
    private Map<String, Structure.Parser<?>> parsers;
    private final Map<String, Type> types = Map.ofEntries(
            Map.entry("byte", ByteLit.Analysis.TYPE),
            Map.entry("boolean", BooleanLit.Analysis.TYPE)
    );

    private void resetParsers(String name) {
        unitStx = new CompilationUnit.Parser(name);
        parsers = Map.ofEntries(
                Map.entry("compile_with", new CompileWith.Parser(unitStx)),
                Map.entry("compile_settings", new CompileSettings.Parser()),
                Map.entry("comment", new Comment.Parser()),
                Map.entry("debug", new Debug.Parser()),
                Map.entry("function", new Func.Parser()),
                Map.entry("function_call", new FuncCall.Parser()),
                Map.entry("paren_params_decl", new ParenParamsDecl.Parser()),
                Map.entry("var_param_decl", new VarParamDecl.Parser()),
                Map.entry("return", new Return.Parser()),
                Map.entry("var_decl", new VarDecl.Parser()),
                Map.entry("var_get", new VarGet.Parser()),
                Map.entry("type_decl", new TypeDecl.Parser()),
                Map.entry("static_type_get", new StaticTypeGet.Parser()),
                Map.entry("ref_type_get", new RefTypeGet.Parser()),
                Map.entry("byte_lit", new ByteLit.Parser()),
                Map.entry("float32_lit", new Float32Lit.Parser()),
                Map.entry("float64_lit", new Float64Lit.Parser()),
                Map.entry("int32_lit", new Int32Lit.Parser()),
                Map.entry("int64_lit", new Int64Lit.Parser()),
                Map.entry("boolean_lit", new BooleanLit.Parser()),
                Map.entry("char_lit", new CharLit.Parser()),
                Map.entry("string_lit", new StringLit.Parser()),
                Map.entry("print", new Print.Parser()),
                Map.entry("if", new If.Parser()),
                Map.entry("while", new While.Parser()),
                Map.entry("break", new Break.Parser()),
                Map.entry("add", new Add.Parser()),
                Map.entry("subtract", new Subtract.Parser()),
                Map.entry("multiply", new Multiply.Parser()),
                Map.entry("divide", new Divide.Parser()),
                Map.entry("modulo", new Modulo.Parser()),
                Map.entry("or", new Or.Parser()),
                Map.entry("and", new And.Parser()),
                Map.entry("xor", new Xor.Parser()),
                Map.entry("lshift", new LShift.Parser()),
                Map.entry("rshift", new RShift.Parser()),
                Map.entry("add_set", new Add.SetParser()),
                Map.entry("subtract_set", new Subtract.SetParser()),
                Map.entry("multiply_set", new Multiply.SetParser()),
                Map.entry("divide_set", new Divide.SetParser()),
                Map.entry("modulo_set", new Modulo.SetParser()),
                Map.entry("or_set", new Or.SetParser()),
                Map.entry("and_set", new And.SetParser()),
                Map.entry("xor_set", new Xor.SetParser()),
                Map.entry("lshift_set", new LShift.SetParser()),
                Map.entry("rshift_set", new RShift.SetParser()),
                Map.entry("equals", new Equals.Parser()),
                Map.entry("greater", new Greater.Parser()),
                Map.entry("lesser", new Lesser.Parser()),
                Map.entry("scan", new Scan.Parser()),
                Map.entry("time", new Time.Parser()),
                Map.entry("debug_flag", new DebugFlag.Parser()),
                Map.entry("malloc", new Malloc.Parser()),
                Map.entry("malloc_set", new MallocSet.Parser()),
                Map.entry("free", new Free.Parser()),
                Map.entry("ref", new Ref.Parser()),
                Map.entry("deref", new Deref.Parser()),
                Map.entry("array_deref", new ArrayDeref.Parser()),
                Map.entry("set", new Set.Parser()),
                Map.entry("increment", new Increment.Parser()),
                Map.entry("decrement", new Decrement.Parser()),
                Map.entry("cast", new Cast.Parser()),
                Map.entry("toFloat", new IntToFloat.Parser()),
                Map.entry("toInt", new FloatToInt.Parser())
        );
    }

    public Result compile(File file) throws IOException {
        if (!file.exists()) return null;
        if (file.isDirectory()) return null;

        CharIterator chars = new CharIterator(file);
        return compile(bytecodeName(file), chars, bytecodeTarget(file));
    }

    private static String bytecodeName(File file) {
        return StringUtils.nameNoExt(file) + "." + Compiler.BYTECODE_EXT;
    }

    public Result compile(String name, CharIterator chars, Path target) throws IOException {
        Result result = new Result();
        var unitRawRes = parse(name, chars, result);

        if (unitRawRes.success()) {
            var unitRaw = unitRawRes.valueNonNull();
            if (analyze(unitRaw, result)) {
                assemble(result.unit, result);
                result.chunk.writeTo(target);
            }
        }

        return result; 
    }

    protected Structure.Parser.Result<? extends CompilationUnit.Raw> parse(String name, CharIterator chars, Result result) {
        resetParsers(name);
        result.syntaxCxt = new ParseContext(chars, new DiagnosticList(), parsers);
        var unitRes = unitStx.parse(result.syntaxCxt);
        if (unitRes.hasValue()) result.syntaxCxt.addCompilationUnit(unitRes.valueNonNull());
        return unitRes;
    }

    protected boolean analyze(CompilationUnit.Raw unitRaw, Result result) {
        result.analysisCtx = new AnalysisContext(result.syntaxCxt.compilationUnits, result.syntaxCxt.chars);
        result.unit = unitRaw.analyze(result.analysisCtx);
        return result.analysisCtx.problems.isEmpty();
    }

    private void assemble(CompilationUnit.Analysis analysis, Result result) {
        result.assembleCtx = new AssembleContext();
        analysis.assemble(result.assembleCtx);
        result.chunk = result.assembleCtx.assemble();
    }

    public Optional<Structure.Parser<?>> getParser(String name) {
        return Optional.ofNullable(parsers.get(name));
    }

    public static class Result {
        public ParseContext syntaxCxt;
        public AnalysisContext analysisCtx;
        public CompilationUnit.Analysis unit;
        public AssembleContext assembleCtx;
        public Chunk chunk;
    }

    private static Path bytecodeTarget(File file) {
        return file.toPath().resolveSibling(StringUtils.nameNoExt(file) + '.' + BYTECODE_EXT);
    }
    private static boolean isSourceFile(File file) {
        return SOURCE_EXT.equals(StringUtils.extNoName(file));
    }
}
