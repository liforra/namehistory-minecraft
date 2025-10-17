# Contributing to Name History

Thank you for your interest in contributing to Name History! 💜

## How to Contribute

### Reporting Bugs

If you find a bug, please create an issue with:
- A clear, descriptive title
- Steps to reproduce the issue
- Expected behavior vs actual behavior
- Your Minecraft version, mod version, and any other relevant mods
- Screenshots or logs if applicable

### Suggesting Features

Feature requests are welcome! Please:
- Check if the feature has already been requested
- Describe the feature and why it would be useful
- Provide examples or mockups if possible

### Code Contributions

1. **Fork the repository**
2. **Create a feature branch** (`git checkout -b feature/amazing-feature`)
3. **Make your changes**
   - Follow the existing code style (Kotlin conventions)
   - Add comments for complex logic
   - Test your changes thoroughly
4. **Commit your changes** (`git commit -m 'Add amazing feature'`)
5. **Push to your branch** (`git push origin feature/amazing-feature`)
6. **Open a Pull Request**

### Code Style

- Use Kotlin naming conventions
- Keep functions focused and concise
- Add KDoc comments for public APIs
- Follow the existing project structure

### Testing

Before submitting a PR:
- Test the mod in-game
- Verify all features work as expected
- Check that your changes don't break existing functionality
- Test with ModMenu if UI changes are involved

### License

By contributing, you agree that your contributions will be licensed under the AGPL-3.0 license.

## Development Setup

1. Clone the repository
2. Open in IntelliJ IDEA or your preferred IDE
3. Run `./gradlew genSources` to generate Minecraft sources
4. Run `./gradlew runClient` to test in-game
5. Build with `./gradlew build`

## Questions?

Feel free to open an issue for any questions about contributing!
