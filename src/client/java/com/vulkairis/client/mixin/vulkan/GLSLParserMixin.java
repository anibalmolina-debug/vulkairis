package com.vulkairis.client.mixin.vulkan;

import net.vulkanmod.vulkan.shader.converter.Attribute;
import net.vulkanmod.vulkan.shader.converter.GLSLParser;
import net.vulkanmod.vulkan.shader.converter.Token;
import net.vulkanmod.vulkan.shader.converter.UniformBlock;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Mixin into VulkanMod's GLSLParser to prevent IllegalStateException
 * when parsing complex Iris shaders with arrays, qualifiers, custom blocks, or legacy attributes.
 */
@Mixin(value = GLSLParser.class, remap = false)
public abstract class GLSLParserMixin {
    @Shadow private Token currentToken;
    @Shadow private List<Token> tokens;
    @Shadow private int currentTokenIdx;
    @Shadow private GLSLParser.Stage stage;
    @Shadow int currentOutAtt;
    @Shadow ArrayList<Attribute> vertInAttributes;
    @Shadow ArrayList<Attribute> vertOutAttributes;
    @Shadow ArrayList<Attribute> fragInAttributes;
    @Shadow ArrayList<Attribute> fragOutAttributes;
    @Shadow @Final Set<UniformBlock.Field> legacyUniforms;
    @Shadow List<UniformBlock> uniformBlocks;

    @Shadow abstract void advanceToken(boolean skipSpacing);
    @Shadow abstract GLSLParser.TokenNode prevNode(boolean skipSpacing);
    @Shadow abstract Attribute getVertAttribute(Attribute attribute);
    @Shadow abstract void appendNode(GLSLParser.Node node);

    /**
     * @author Vulkairis
     * @reason Support array uniforms, qualifiers, and extra syntax without throwing IllegalStateException.
     */
    @Overwrite
    private UniformBlock.Field parseUniformField() {
        if (this.currentToken == null) {
            return null;
        }

        while (this.currentToken.type != Token.TokenType.IDENTIFIER &&
               this.currentToken.type != Token.TokenType.SEMICOLON &&
               this.currentToken.type != Token.TokenType.EOF) {
            this.advanceToken(true);
        }

        if (this.currentToken.type == Token.TokenType.SEMICOLON || this.currentToken.type == Token.TokenType.EOF) {
            return null;
        }

        String fieldType = this.currentToken.value;
        this.advanceToken(true);

        String fieldName = "unknown";
        if (this.currentToken.type == Token.TokenType.IDENTIFIER) {
            fieldName = this.currentToken.value;
            this.advanceToken(true);
        }

        // Consume any array brackets [4], initializers, etc. until semicolon or EOF
        while (this.currentToken.type != Token.TokenType.SEMICOLON && this.currentToken.type != Token.TokenType.EOF) {
            this.advanceToken(true);
        }

        return new UniformBlock.Field(fieldType, fieldName);
    }

    /**
     * @author Vulkairis
     * @reason Null-safe parseStorageUniform.
     */
    @Overwrite
    private void parseStorageUniform() {
        UniformBlock.Field field = this.parseUniformField();
        if (field != null && this.legacyUniforms != null) {
            this.legacyUniforms.add(field);
        }
    }

    /**
     * @author Vulkairis
     * @reason Handle layout(...) in/out/buffer declarations from Iris shaders without throwing IllegalStateException.
     */
    @Overwrite
    private void parseUniformBlock() {
        // currentToken is "layout" when this is called
        this.advanceToken(true); // move past 'layout'

        // Expect LEFT_PARENTHESIS for layout(...)
        if (this.currentToken == null || this.currentToken.type != Token.TokenType.LEFT_PARENTHESIS) {
            skipDeclaration();
            return;
        }

        // Consume everything inside layout(...) until RIGHT_PARENTHESIS
        while (this.currentToken != null
               && this.currentToken.type != Token.TokenType.RIGHT_PARENTHESIS
               && this.currentToken.type != Token.TokenType.EOF) {
            this.advanceToken(true);
        }

        // Advance past RIGHT_PARENTHESIS
        this.advanceToken(true);

        if (this.currentToken == null) return;

        String keyword = this.currentToken.value;

        // Case 1: layout-qualified attribute (in/out) — delegate to parseAttribute
        if ("in".equalsIgnoreCase(keyword) || "out".equalsIgnoreCase(keyword)) {
            this.parseAttribute();
            return;
        }

        // Case 2: not "uniform" — unknown layout-qualified decl (buffer, readonly, etc.) — skip
        if (!"uniform".equals(keyword)) {
            skipDeclaration();
            return;
        }

        // Case 3: layout(...) uniform — could be a block or standalone uniform
        this.advanceToken(true); // consume "uniform"

        if (this.currentToken == null || this.currentToken.type != Token.TokenType.IDENTIFIER) {
            skipDeclaration();
            return;
        }

        String blockName = this.currentToken.value;
        this.advanceToken(true); // consume block name

        // If no LEFT_BRACE follows, it's a standalone uniform (e.g. layout(...) uniform sampler2D tex;)
        if (this.currentToken == null || this.currentToken.type != Token.TokenType.LEFT_BRACE) {
            skipDeclaration();
            return;
        }

        // Parse uniform block body: { field; field; ... }
        UniformBlock block = new UniformBlock(blockName);
        this.advanceToken(true); // consume '{'

        while (this.currentToken != null
               && this.currentToken.type != Token.TokenType.RIGHT_BRACE
               && this.currentToken.type != Token.TokenType.EOF) {
            UniformBlock.Field field = this.parseUniformField();
            if (field != null) {
                block.addField(field);
            }
            this.advanceToken(true);
        }

        // Advance past '}'
        this.advanceToken(true);

        // Handle optional alias identifier before semicolon
        if (this.currentToken != null && this.currentToken.type == Token.TokenType.IDENTIFIER) {
            block.setAlias(this.currentToken.value);
            this.advanceToken(true);
        }

        // Expect semicolon — skip if not present
        if (this.currentToken != null && this.currentToken.type != Token.TokenType.SEMICOLON) {
            skipDeclaration();
        }

        // Register the block
        if (this.uniformBlocks != null) {
            this.uniformBlocks.add(block);
        }

        // Consume trailing newline
        consumeTrailingNewline();
    }

    /**
     * @author Vulkairis
     * @reason Robustly parse in/out/attribute declarations with qualifiers, layout tags, or arrays without throwing IllegalStateException.
     */
    @Overwrite
    private void parseAttribute() {
        GLSLParser.TokenNode prev = this.prevNode(true);
        if (prev != null) {
            String prevVal = prev.getStringValue();
            if ("(".equals(prevVal) || ",".equals(prevVal)) {
                return;
            }
        }

        if (this.currentToken == null) return;

        String rawIo = this.currentToken.value != null ? this.currentToken.value : "in";
        String ioType = "attribute".equalsIgnoreCase(rawIo) ? "in" : rawIo;

        this.advanceToken(true);

        // Collect identifier tokens until semicolon or EOF
        List<String> identifiers = new ArrayList<>();
        while (this.currentToken != null &&
               this.currentToken.type != Token.TokenType.SEMICOLON &&
               this.currentToken.type != Token.TokenType.EOF) {
            if (this.currentToken.type == Token.TokenType.IDENTIFIER) {
                String val = this.currentToken.value;
                if (!isQualifier(val)) {
                    identifiers.add(val);
                }
            }
            this.advanceToken(true);
        }

        if (identifiers.isEmpty()) {
            return;
        }

        String type;
        String name;
        if (identifiers.size() == 1) {
            type = "vec4";
            name = identifiers.get(0);
        } else {
            type = identifiers.get(identifiers.size() - 2);
            name = identifiers.get(identifiers.size() - 1);
        }

        // Consume trailing newline if present in spacing
        if (this.tokens != null && this.currentTokenIdx < this.tokens.size()) {
            Token nextToken = this.tokens.get(this.currentTokenIdx);
            if (nextToken != null && nextToken.type == Token.TokenType.SPACING && nextToken.value != null) {
                if ("\n".equals(nextToken.value)) {
                    this.currentTokenIdx++;
                } else {
                    int idx = nextToken.value.indexOf("\n");
                    if (idx >= 0) {
                        nextToken.value = nextToken.value.substring(idx + 1);
                    }
                }
            }
        }

        Attribute attribute = new Attribute(ioType, type, name);

        if (this.stage == GLSLParser.Stage.VERTEX) {
            if ("in".equalsIgnoreCase(ioType)) {
                attribute.setLocation(-1);
                if (this.vertInAttributes != null) {
                    this.vertInAttributes.add(attribute);
                }
            } else if ("out".equalsIgnoreCase(ioType)) {
                attribute.setLocation(this.currentOutAtt++);
                if (this.vertOutAttributes != null) {
                    this.vertOutAttributes.add(attribute);
                }
            }
        } else if (this.stage == GLSLParser.Stage.FRAGMENT) {
            if ("in".equalsIgnoreCase(ioType)) {
                Attribute vertAtt = this.getVertAttribute(attribute);
                if (vertAtt == null) {
                    return;
                }
                int loc = ((AttributeAccessor) vertAtt).getLocation();
                attribute.setLocation(loc);
                if (this.fragInAttributes != null) {
                    this.fragInAttributes.add(attribute);
                }
            } else if ("out".equalsIgnoreCase(ioType)) {
                attribute.setLocation(this.currentOutAtt++);
                if (this.fragOutAttributes != null) {
                    this.fragOutAttributes.add(attribute);
                }
            }
        }

        this.appendNode(attribute);
    }

    /**
     * Skip to the end of a declaration, handling brace-enclosed blocks (e.g. buffer { ... };).
     */
    private void skipDeclaration() {
        int braceDepth = 0;
        while (this.currentToken != null && this.currentToken.type != Token.TokenType.EOF) {
            if (this.currentToken.type == Token.TokenType.LEFT_BRACE) braceDepth++;
            if (this.currentToken.type == Token.TokenType.RIGHT_BRACE) braceDepth--;
            if (this.currentToken.type == Token.TokenType.SEMICOLON && braceDepth <= 0) break;
            this.advanceToken(true);
        }
    }

    /**
     * Consume trailing newline spacing token so the declaration is cleanly removed from output.
     */
    private void consumeTrailingNewline() {
        if (this.tokens != null && this.currentTokenIdx < this.tokens.size()) {
            Token nextToken = this.tokens.get(this.currentTokenIdx);
            if (nextToken != null && nextToken.type == Token.TokenType.SPACING && nextToken.value != null) {
                if ("\n".equals(nextToken.value)) {
                    this.currentTokenIdx++;
                } else {
                    int idx = nextToken.value.indexOf("\n");
                    if (idx >= 0) {
                        nextToken.value = nextToken.value.substring(idx + 1);
                    }
                }
            }
        }
    }

    private static boolean isQualifier(String val) {
        if (val == null) return false;
        return switch (val) {
            case "highp", "mediump", "lowp", "precision",
                 "flat", "smooth", "noperspective",
                 "centroid", "sample", "patch",
                 "invariant", "coherent", "volatile",
                 "restrict", "readonly", "writeonly" -> true;
            default -> false;
        };
    }
}
