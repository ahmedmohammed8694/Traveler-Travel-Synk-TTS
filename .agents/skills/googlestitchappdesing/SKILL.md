---
name: googlestitchappdesing
description: Integrate and leverage Google Stitch MCP for automated application design, UI prototyping, visual component generation, and design system alignment. Use when designing mobile and web application interfaces, prototyping screen layouts, or connecting with Google Stitch AI design tools.
---

# Google Stitch App Design (`googlestitchappdesing`)

This skill integrates Google Stitch MCP services (`https://stitch.googleapis.com/mcp`) into Antigravity to enable automated UI/UX design, application layout generation, component styling, and visual application prototyping.

## Overview & Capabilities

Google Stitch allows AI agents to interact directly with Google's application design infrastructure. With `googlestitchappdesing`, you can:
- Generate application screen layouts, wireframes, and design specs.
- Design responsive UI components adhering to modern design systems (Material Design, custom design tokens, micro-animations).
- Sync design assets, color schemes, typography, and component structures across projects.
- Automate UI code generation for Flutter, Jetpack Compose, React, HTML/CSS, and Web Components.

## Configuration Details

The Google Stitch MCP Server is configured in `mcp_config.json`:
- **Server URL**: `https://stitch.googleapis.com/mcp`
- **Header**: `X-Goog-Api-Key`

## Workflow & Usage

1. **Triggering the Skill**:
   - Type `/googlestitchappdesing` in chat or invoke it during design and UI prototyping tasks.

2. **Design Generation & Prototyping**:
   - Provide application requirements, screen descriptions, or user stories.
   - Stitch will generate structured design tokens, UI component specifications, and responsive layout recommendations.

3. **Code & Component Conversion**:
   - Seamlessly convert Stitch design output into production-ready frontend code (Flutter widgets, Jetpack Compose UI, React/Vite web components, or Vanilla HTML/CSS).
