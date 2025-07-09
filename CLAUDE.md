# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Magic: The Gathering Card Inventory Android App

This repository contains an Android application for scanning, organizing, and managing Magic: The Gathering card collections. The app enables users to scan multiple cards simultaneously using collector numbers and set codes, organize their collection into sets and decks, track card values, and manage their inventory.

### Project Structure

```
MTGInventoryApp/
├── app/
│   ├── src/main/
│   │   ├── java/com/mtginventory/app/
│   │   │   ├── ui/                  # Jetpack Compose UI
│   │   │   │   ├── scanner/         # Camera and scanning screens
│   │   │   │   ├── collection/      # Collection management screens
│   │   │   │   ├── deck/            # Deck builder screens
│   │   │   │   ├── analytics/       # Analytics and insights
│   │   │   │   ├── theme/           # App theming
│   │   │   │   └── MainActivity.kt  # Main activity
│   │   │   ├── data/                # Data layer
│   │   │   │   ├── database/        # Room database
│   │   │   │   ├── repository/      # Repository pattern
│   │   │   │   └── api/             # API services
│   │   │   ├── viewmodel/           # ViewModels
│   │   │   ├── model/               # Data models
│   │   │   └── di/                  # Dependency injection
│   │   ├── res/                     # Resources
│   │   │   ├── layout/              # XML layouts (if needed)
│   │   │   ├── values/              # Colors, strings, styles
│   │   │   ├── drawable/            # Images and drawables
│   │   │   └── mipmap/              # App icons
│   │   └── AndroidManifest.xml      # App manifest
│   └── build.gradle                 # App-level build config
├── gradle/                          # Gradle wrapper
├── build.gradle                     # Project-level build config
└── settings.gradle                  # Project settings
```

### Core Features

**Phase 1 - Core Scanning & Inventory:**
- **Bulk binder scanning** - scan 9-12 cards at once (3x3 or 4x3 layouts)
- Multi-card scanning using device camera with intelligent card detection
- ML Kit text recognition for collector numbers and set codes
- Card identification via Scryfall API
- Basic collection management and organization
- Room database persistence for offline access

**Phase 2 - Enhanced Management:**
- Set and deck organization
- Price tracking integration (TCGPlayer, Scryfall)
- Collection statistics and analytics
- Export/import functionality
- Search and filtering capabilities

**Phase 3 - Advanced Features:**
- Deck builder with suggestions
- Price alerts and tracking
- Marketplace integration
- Collection sharing and social features
- Trade management system

### Technology Stack

- **Platform**: Android 7.0+ (API 24+)
- **Language**: Kotlin
- **UI Framework**: Jetpack Compose
- **Architecture**: MVVM with Repository pattern
- **Database**: Room (SQLite)
- **Camera**: CameraX
- **Text Recognition**: ML Kit
- **Networking**: Retrofit + OkHttp
- **Image Loading**: Coil
- **Dependency Injection**: Hilt
- **Testing**: JUnit, Espresso, Compose Testing

### Key APIs and Services

**Scryfall API** (Primary card database):
- Endpoint: `https://api.scryfall.com`
- Rate limit: 50-100ms between requests (10 req/sec)
- Free tier with comprehensive MTG data
- Bulk data downloads for offline caching
- Card images and metadata

**TCGPlayer API** (Price data):
- Endpoint: `https://api.tcgplayer.com`
- Requires API key authentication
- Real-time pricing information
- Market data and trends

### Development Commands

```bash
# Build the project
./gradlew assembleDebug

# Run tests
./gradlew test

# Run instrumented tests
./gradlew connectedAndroidTest

# Build release APK
./gradlew assembleRelease

# Install debug APK
./gradlew installDebug

# Clean build
./gradlew clean

# Run lint checks
./gradlew lint

# Generate test coverage report
./gradlew jacocoTestReport
```

### Architecture Patterns

**MVVM with Repository Pattern:**
- **Views**: Jetpack Compose screens with minimal business logic
- **ViewModels**: Handle UI state and business logic
- **Repository**: Data access abstraction layer
- **Data Sources**: Room database and API services

**Data Flow:**
1. User scans cards using CameraX
2. ML Kit extracts text (collector numbers, set codes)
3. Repository identifies cards using Scryfall API
4. ViewModels update UI state
5. Room database persists data for offline access

### Card Identification Process

**Collector Number Format:**
- Modern cards: 4-digit format (e.g., "0123")
- Legacy cards: Fraction format (e.g., "123/250")
- Rarity indicators: C (common), U (uncommon), R (rare), M (mythic)

**Set Code Format:**
- 3-letter set code (e.g., "DMU" for Dominaria United)
- 2-letter language code (e.g., "EN" for English)
- Format: "DMU•EN" (separated by bullet point)

**Bulk Binder Scanning Strategy:**
1. **Grid Detection**: Use CameraX to detect 3x3 or 4x3 card grids in binder pages
2. **Individual Card Isolation**: Automatically detect and isolate each card within the grid
3. **Parallel Text Recognition**: Use ML Kit to simultaneously extract text from all cards
4. **Smart Parsing**: Extract collector numbers, set codes, and card names from each position
5. **Batch API Processing**: Send parallel requests to Scryfall for efficient card identification
6. **Validation & Correction**: Cross-reference results and handle edge cases (foils, promos, alternate versions)
7. **Progress Tracking**: Show real-time progress for each card in the grid

### Data Models

**Room Database Entities:**
- `Card`: Individual card with properties, prices, quantity
- `MTGSet`: Set information (name, code, release date)
- `Collection`: User-defined collections and decks
- `PriceHistory`: Historical price tracking
- `ScanSession`: Batch scanning session data

**Key Properties:**
- Card: scryfallId, name, setCode, collectorNumber, rarity, quantity, lastUpdated
- Collection: name, type (set/deck), cards, createdDate, totalValue
- PriceHistory: cardId, source, price, currency, timestamp

### Performance Considerations

**Camera and ML Kit:**
- Use CameraX for efficient camera management with high-resolution capture
- Implement **computer vision for grid detection** (3x3/4x3 card layouts)
- **Parallel processing** for simultaneous card recognition
- Smart **caching and deduplication** to avoid re-processing same cards
- **Optimized batch API requests** to respect rate limits while maximizing throughput
- **Progressive scanning** - users can scan multiple binder pages in sequence

**Room Database:**
- Use coroutines for database operations
- Implement proper pagination for large collections
- Use database views for complex queries
- Regular cleanup of old price data

**API Management:**
- Implement proper rate limiting (50-100ms between requests)
- Cache API responses for 24 hours minimum
- Use OkHttp interceptors for caching and retry logic
- Implement proper error handling and user feedback

### Security and Privacy

**API Key Management:**
- Store API keys in `local.properties` (not in version control)
- Use BuildConfig for accessing keys
- Implement proper key rotation strategies

**User Privacy:**
- Request camera permissions appropriately
- Implement proper data handling for user collections
- Allow data export and deletion
- Follow Android privacy guidelines

### Testing Strategy

**Unit Tests:**
- Repository and API service testing
- Room database testing
- ViewModel testing with test doubles
- Business logic validation

**Integration Tests:**
- Full scanning workflow testing
- Database integration testing
- API integration with mock server

**UI Tests:**
- Compose UI testing
- Camera functionality testing
- Navigation testing
- Accessibility testing

### Configuration

**Required permissions in AndroidManifest.xml:**
- `android.permission.CAMERA`
- `android.permission.INTERNET`
- `android.permission.ACCESS_NETWORK_STATE`

**Build configuration:**
- Minimum SDK: 24 (Android 7.0)
- Target SDK: 34 (Android 14)
- Compile SDK: 34
- JVM Target: 1.8

### Future Enhancements

**Machine Learning:**
- Custom TensorFlow Lite models for card recognition
- Improved OCR accuracy for damaged cards
- Auto-categorization of collections
- Price prediction algorithms

**Advanced Features:**
- Augmented Reality card overlay
- Blockchain/NFT integration
- Advanced analytics and insights
- Community features and trading

### Important Notes

- **Binder Scanning Optimization**: Test extensively with real card binders to ensure accurate grid detection
- Always test camera functionality on physical devices with various lighting conditions
- **Multi-card Processing**: Implement robust error handling for partial scan results (some cards successful, others failed)
- Follow Material Design 3 guidelines for UI consistency
- **Batch Operations**: Design UI to handle 9-12 card results simultaneously without overwhelming the user
- Consider accessibility features for visually impaired users
- Implement proper data backup and sync capabilities for large collections
- Use Android 7.0+ for ML Kit text recognition features
- **Battery Optimization**: Critical for bulk scanning sessions - implement efficient processing and camera management
- Plan for different device capabilities (camera quality, processing power, screen size for grid visualization)
- **Binder Page Management**: Consider sequential page scanning workflows for complete collection digitization

## Development Workflow

1. **Start with core scanning functionality** using CameraX + ML Kit
2. **Implement Scryfall API integration** for card identification
3. **Set up Room database** for persistence
4. **Build collection management screens** with Jetpack Compose
5. **Add price tracking** via TCGPlayer API
6. **Implement search and filtering** capabilities
7. **Add deck building features** and suggestions
8. **Integrate marketplace features** and trading
9. **Optimize performance** and add advanced features
10. **Prepare for Google Play Store submission**

## 🔥 **Key Selling Point: Bulk Binder Scanning**

### Revolutionary Multi-Card Detection
The app's standout feature is its ability to **scan entire binder pages at once**:

**📸 Bulk Scanning Capability:**
- **3x3 Grid Scanning**: Perfect for standard 9-pocket binder pages
- **4x3 Grid Scanning**: Ideal for 12-pocket binder pages  
- **Intelligent Card Detection**: Automatically identifies and isolates each card position
- **Parallel Processing**: Processes all cards simultaneously for maximum speed
- **Progress Visualization**: Real-time feedback showing each card's identification status

**🎯 Target User Experience:**
```
"Just point your phone at a binder page and scan 9-12 cards instantly!"
```

**📊 Efficiency Gains:**
- **Traditional**: 1 card per scan = 100+ scans for large collections
- **MTG Inventory App**: 9-12 cards per scan = 10-15 scans for same collection
- **Time Savings**: 90% reduction in scanning time
- **User Experience**: Seamless bulk collection digitization

**🛠️ Technical Implementation Priority:**
1. **Phase 1**: Basic grid detection (3x3 layout)
2. **Phase 2**: Advanced grid detection (4x3 + flexible layouts)
3. **Phase 3**: Smart page sequencing for complete binder scanning
4. **Phase 4**: Auto-detection of binder page layouts

This bulk scanning capability makes the app **10x faster** than competitors and positions it as the **#1 choice for serious MTG collectors** who want to digitize large collections efficiently.

---

This architecture provides a solid foundation for a comprehensive MTG card inventory application with room for future enhancements and marketplace integration.