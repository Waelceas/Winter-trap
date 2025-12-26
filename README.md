WINTER TRAP

Overview Winter Trap is a Minecraft plugin that provides a comprehensive trap management system with Towny integration. It allows towns to create, manage, upgrade, and trade traps through a GUI-based marketplace system.

Core Features

    Trap Management

    Trap Creation: Players can create traps in defined regions using coordinates
    Trap Ownership: Traps are owned by towns, not individual players
    Member System: Town members can have different roles (MEMBER, MOD, ADMIN)
    PVP Control: Traps can have PVP enabled/disabled
    Location Tracking: Each trap has defined boundaries (pos1, pos2)
    Level & Statistics System
    Trap Levels: Traps can be upgraded from level 1 to 10
    Kill Tracking: System tracks total kills per trap
    Performance Metrics: Statistics are stored and managed separately
    Marketplace System
    Central Market Pool: Players sell traps to a central marketplace
    Dynamic Pricing: Prices calculated based on trap level and kill count
    Player-to-Player Trading: Players buy from the market pool, not directly from sellers
    Market Persistence: Market data saved in trapmarket.yml

    GUI Interface
    
    Main Menu: Central hub for all trap operations
    Buy Menu: Browse and purchase traps from marketplace
    Sell Menu: List owned traps for sale to marketplace
    My Traps Menu: View and manage owned traps
    Upgrade Menu: Upgrade trap levels with costs
    Economy Integration
    Vault Support: Uses Vault for economy operations
    Towny Integration: Works with Towny town bank accounts
    Dynamic Pricing: Configurable pricing formulas
    Tax System: Periodic tax collection from traps

Architecture

Core Classes

    TrapSystem: Main plugin class
    Trap: Model representing a trap with location, ownership, and metadata
    TrapStats: Statistics tracking for each trap
    TrapMarket: Central marketplace management system

Management System

    ManagerRegistry: Central dependency injection container
    TrapManager: Core trap data management
    TrapLevelManager: Level and statistics management
    TrapMarket: Marketplace operations

GUI System

    TrapMainMenuGUI: Main interface
    TrapBuyGUI: Purchase interface
    TrapSellGUI: Selling interface
    TrapUpgradeGUI: Upgrade interface

Handler System

    TrapShopActionHandler: Central action coorinator
    TrapShopBuyHandler: Purchase operations
    TrapShopSellHandler: Selling operations
    TrapShopUpgradeHandler: Upgrade operations


Commands

    /trapshop: Opens main trap shop menu
    /trap: Basic trap management commands
    /trapadmin: Administrative commands

Marketplace Flow

Selling Process

    Player selects trap from "My Traps"
    System calculates market price based on level/kills
    Trap ownership is cleared and added to market pool
    Other players can now purchase this trap

Buying Process

    Player browses available traps in market
    System displays seller town, price, and location
    Player purchases trap with economy withdrawal
    Trap ownership transferred to buyer's town
    Trap removed from marketplace


Data Structure

    Each trap has unique ID, coordinates, ownership
    Statistics tracked separately for performance
    Market data includes pricing and seller information

Integration Points

Towny Integration

    Uses Towny API for town validation
    Economy operations through town banks
    Resident-to-town mapping for permissions

Vault Integration

    Economy operations for buying/selling
    Tax collection system
    Upgrade cost management

Key Features Summary

    Decentralized Trading: Players trade through central market, not directly
    Dynamic Pricing: Prices based on trap performance (level, kills)
    Role-Based Access: Different permission levels within towns
    Persistent Storage: All data saved across server restarts
    GUI-Driven: All operations through intuitive interfaces
    Economy Integration: Full economy support with Towny

    Admin Tools: Comprehensive administrative commands

    Tax System: Automatic tax collection from traps
