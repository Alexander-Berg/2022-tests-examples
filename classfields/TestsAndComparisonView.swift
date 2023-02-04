import SwiftUI
import AutoRuFontSchema
import AutoRuColorSchema
import AutoRuAppearance
import AutoRuFetchableImage
import AutoRuSwiftUICore
import AutoRuDesignKitSwiftUI

struct TestsAndComparisonView: View {
    @ObservedObject var model: TestsAndComparisonModel
    
    var body: some View {
        VStack(spacing: 0) {
            
            Text(model.title)
                .fontConfiguration(.h4.bold)
                .frame(maxWidth: .infinity, alignment: .leading)
                .foregroundColor(.text.primary)
                .padding(16)
            
            FiltersListView(model: model.filters)
            
            Spacer()
                .frame(height: 24)
            
            ScrollView(.horizontal, showsIndicators: false) {
                
                HStack(spacing: 8) {
                    ForEach(model.topImagesModels) { model in
                        
                        ZStack(alignment: .bottom) {
                            if let url = model.imageURL {
                                FetchableImageView(
                                    FetchableImage(url: url),
                                    content: { image in
                                        image
                                            .resizable()
                                            .aspectRatio(contentMode: .fill)
                                    },
                                    placeholder: {
                                        Color(ColorSchema.Background.background.resolved(with: .light))
                                })
                            }
                            
                            LinearGradient(colors: [.clear, .black], startPoint: UnitPoint(x: 0.5, y: 0.0), endPoint: UnitPoint(x: 0.5, y: 1.0))
                                .frame(height: 150)
                            
                            Text(model.text)
                                .fontConfiguration(.sub)
                                .foregroundColor(.text.primaryWhite)
                                .lineLimit(nil)
                                .padding([.leading, .trailing, .bottom], 16)
                        }
                        .frame(width: 300, height: 300)
                        .cornerRadius(16)
                        .onAruTapGesture {
                            model.onTap?(model.id)
                        }
                    }
                }
                .padding(.horizontal, 16)
            }
            
            Spacer()
                .frame(height: 24)
            
            VStack(spacing: 24) {
                
                ForEach(model.choosenModels) { row in
                    TextImageModelsView(row: row)
                }
            }
            
            Spacer()
                .frame(height: 24)
            
            AruButton(model.bottomButton.text ?? "", shouldStretch: true, onTap: {
                model.bottomButton.onTap?()
            })
            .aruButtonStyle(color: .black)
            .aruButtonSize(.medium)
            .padding(.horizontal, 16)
            
            Spacer()
                .frame(height: 16)
        }
        .frame(maxWidth: .infinity)
        .background(Color.background.surface)
        .cornerRadius(24)
    }
}
