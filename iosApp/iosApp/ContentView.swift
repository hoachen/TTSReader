//
//  ContentView.swift
//  iosApp
//
//  Created by TTSReader Team on 2024.
//

import SwiftUI

struct ContentView: View {
    var body: some View {
        TabView {
            HomeView()
                .tabItem {
                    Image(systemName: "house.fill")
                    Text("主页")
                }
            
            LibraryView()
                .tabItem {
                    Image(systemName: "books.vertical")
                    Text("书库")
                }
            
            CameraView()
                .tabItem {
                    Image(systemName: "camera")
                    Text("扫描")
                }
            
            SettingsView()
                .tabItem {
                    Image(systemName: "gearshape")
                    Text("设置")
                }
        }
        .accentColor(.blue)
    }
}

struct HomeView: View {
    var body: some View {
        NavigationView {
            VStack(spacing: 20) {
                Text("TTS Reader")
                    .font(.largeTitle)
                    .fontWeight(.bold)
                
                Text("欢迎使用文本转语音阅读器")
                    .font(.title3)
                    .foregroundColor(.secondary)
                
                Spacer()
                
                VStack(spacing: 16) {
                    Button(action: {
                        // TODO: Start reading
                    }) {
                        Text("开始朗读")
                            .font(.headline)
                            .frame(maxWidth: .infinity)
                            .padding()
                            .background(Color.blue)
                            .foregroundColor(.white)
                            .cornerRadius(10)
                    }
                    
                    Button(action: {
                        // TODO: Open file picker
                    }) {
                        Text("选择文本文件")
                            .font(.headline)
                            .frame(maxWidth: .infinity)
                            .padding()
                            .background(Color(UIColor.systemGray5))
                            .cornerRadius(10)
                    }
                }
                .padding(.horizontal)
                
                Spacer()
            }
            .navigationTitle("主页")
        }
    }
}

struct LibraryView: View {
    var body: some View {
        NavigationView {
            VStack {
                Text("书库")
                    .font(.largeTitle)
                    .fontWeight(.bold)
                
                Spacer()
                
                Text("暂无文件")
                    .foregroundColor(.secondary)
                
                Spacer()
            }
            .navigationTitle("书库")
        }
    }
}

struct CameraView: View {
    var body: some View {
        NavigationView {
            VStack(spacing: 30) {
                Text("相机扫描")
                    .font(.largeTitle)
                    .fontWeight(.bold)
                
                Image(systemName: "camera.viewfinder")
                    .font(.system(size: 80))
                    .foregroundColor(.blue)
                
                Text("使用相机扫描文本")
                    .font(.title3)
                    .foregroundColor(.secondary)
                
                VStack(spacing: 16) {
                    Button(action: {
                        // TODO: Open camera
                    }) {
                        Text("打开相机")
                            .font(.headline)
                            .frame(maxWidth: .infinity)
                            .padding()
                            .background(Color.blue)
                            .foregroundColor(.white)
                            .cornerRadius(10)
                    }
                    
                    Button(action: {
                        // TODO: Select from gallery
                    }) {
                        Text("从相册选择")
                            .font(.headline)
                            .frame(maxWidth: .infinity)
                            .padding()
                            .background(Color(UIColor.systemGray5))
                            .cornerRadius(10)
                    }
                }
                .padding(.horizontal)
                
                Spacer()
            }
            .padding()
            .navigationTitle("扫描")
        }
    }
}

struct SettingsView: View {
    var body: some View {
        NavigationView {
            Form {
                Section(header: Text("TTS 设置")) {
                    NavigationLink(destination: EmptyView()) {
                        HStack {
                            Image(systemName: "speaker.wave.2")
                            Text("TTS 提供商")
                            Spacer()
                            Text("MiniMax")
                                .foregroundColor(.secondary)
                        }
                    }
                    
                    NavigationLink(destination: EmptyView()) {
                        HStack {
                            Image(systemName: "text.bubble")
                            Text("AI 文本处理")
                            Spacer()
                            Text("DeepSeek")
                                .foregroundColor(.secondary)
                        }
                    }
                }
                
                Section(header: Text("语音设置")) {
                    NavigationLink(destination: EmptyView()) {
                        HStack {
                            Image(systemName: "slider.horizontal.3")
                            Text("语速、音量、音调")
                        }
                    }
                }
                
                Section(header: Text("外观")) {
                    NavigationLink(destination: EmptyView()) {
                        HStack {
                            Image(systemName: "paintbrush")
                            Text("主题设置")
                            Spacer()
                            Text("跟随系统")
                                .foregroundColor(.secondary)
                        }
                    }
                }
                
                Section(header: Text("存储")) {
                    Button(action: {
                        // TODO: Clear cache
                    }) {
                        HStack {
                            Image(systemName: "trash")
                            Text("清理缓存")
                        }
                    }
                }
            }
            .navigationTitle("设置")
        }
    }
}

struct ContentView_Previews: PreviewProvider {
    static var previews: some View {
        ContentView()
    }
}