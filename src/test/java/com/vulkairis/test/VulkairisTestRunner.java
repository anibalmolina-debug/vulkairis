package com.vulkairis.test;

public class VulkairisTestRunner {
    public static void main(String[] args) {
        System.out.println("=================================================");
        System.out.println("   Vulkairis Prototype Automated Test Suite      ");
        System.out.println("=================================================");

        int passed = 0;
        int failed = 0;

        // 1. Vulkan Capabilities Tests
        try {
            System.out.println("\n[1/4] Running VulkanCapabilitiesTest...");
            VulkanCapabilitiesTest capsTest = new VulkanCapabilitiesTest();
            capsTest.testCapabilitiesDefaults();
            capsTest.testCompatibilityCheck();
            System.out.println(">>> PASS: VulkanCapabilitiesTest");
            passed++;
        } catch (Throwable t) {
            System.err.println(">>> FAIL: VulkanCapabilitiesTest: " + t.getMessage());
            t.printStackTrace();
            failed++;
        }

        // 2. Shader Analyzer & Dry-Run Tests
        try {
            System.out.println("\n[2/4] Running ShaderAnalyzerTest...");
            ShaderAnalyzerTest analyzerTest = new ShaderAnalyzerTest();
            analyzerTest.testValidVertexShaderAnalysis();
            analyzerTest.testValidFragmentShaderAnalysis();
            analyzerTest.testUnsupportedRaytracingShader();
            analyzerTest.testUnsupportedGeometryStage();
            analyzerTest.testMalformedShaderGracefulFailure();
            System.out.println(">>> PASS: ShaderAnalyzerTest");
            passed++;
        } catch (Throwable t) {
            System.err.println(">>> FAIL: ShaderAnalyzerTest: " + t.getMessage());
            t.printStackTrace();
            failed++;
        }

        // 3. Render State Tracker Tests
        try {
            System.out.println("\n[3/4] Running RenderStateTrackerTest...");
            RenderStateTrackerTest trackerTest = new RenderStateTrackerTest();
            trackerTest.testPipelineKeyGeneration();
            trackerTest.testViewportTracking();
            System.out.println(">>> PASS: RenderStateTrackerTest");
            passed++;
        } catch (Throwable t) {
            System.err.println(">>> FAIL: RenderStateTrackerTest: " + t.getMessage());
            t.printStackTrace();
            failed++;
        }

        // 4. In-Process Shaderc & SPIR-V Bytecode Compilation Test
        try {
            System.out.println("\n[4/4] Running ShaderCompilerTest...");
            ShaderCompilerTest compilerTest = new ShaderCompilerTest();
            compilerTest.testInProcessShadercCompilation();
            compilerTest.testCenterDepthCompilation();
            System.out.println(">>> PASS: ShaderCompilerTest");
            passed++;
        } catch (Throwable t) {
            System.err.println(">>> FAIL: ShaderCompilerTest: " + t.getMessage());
            t.printStackTrace();
            failed++;
        }

        System.out.println("\n=================================================");
        System.out.println(String.format("   Test Suite Finished: %d Passed, %d Failed", passed, failed));
        System.out.println("=================================================");

        if (failed > 0) {
            System.exit(1);
        }
    }
}
