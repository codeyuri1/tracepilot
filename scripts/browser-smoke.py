#!/usr/bin/env python3
"""Local UI acceptance: real API, keyboard, themes, axe, reduced motion and responsive layout."""
import argparse
import json
from pathlib import Path
from playwright.sync_api import sync_playwright, expect


def audit(page, axe_source, name):
    page.wait_for_function("document.getAnimations().filter(a => a.effect?.getTiming().iterations !== Infinity).every(a => a.playState !== 'running')")
    page.add_script_tag(path=str(axe_source))
    result = page.evaluate("""async () => {
      const result = await axe.run(document, {runOnly: {type: 'tag', values: ['wcag2a', 'wcag2aa', 'wcag21aa', 'wcag22aa']}});
      return result.violations.map(item => ({id: item.id, impact: item.impact, nodes: item.nodes.map(node => ({target: node.target, summary: node.failureSummary}))}));
    }""")
    assert not result, f'{name} accessibility violations: {json.dumps(result, ensure_ascii=False)}'
    return {'state': name, 'violations': len(result)}


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--url', default='http://127.0.0.1:8081')
    parser.add_argument('--screenshot', type=Path, default=Path('output/playwright/dashboard.png'))
    parser.add_argument('--axe', type=Path, default=Path('frontend/node_modules/axe-core/axe.min.js'))
    args = parser.parse_args()
    args.screenshot.parent.mkdir(parents=True, exist_ok=True)
    errors, audits = [], []
    with sync_playwright() as playwright:
        browser = playwright.chromium.launch()
        page = browser.new_page(viewport={'width': 1600, 'height': 1200}, device_scale_factor=1)
        page.on('pageerror', lambda error: errors.append(str(error)))
        page.goto(args.url)
        expect(page.get_by_role('heading', name='Central de investigação', exact=True)).to_be_visible()
        page.keyboard.press('Tab')
        expect(page.get_by_role('link', name='Pular para o conteúdo')).to_be_focused()
        page.keyboard.press('Enter')
        expect(page.locator('main')).to_be_focused()
        page.keyboard.press('/')
        expect(page.get_by_role('searchbox', name='Buscar incidentes')).to_be_focused()
        expect(page.get_by_role('button', name='Novo incidente')).to_be_enabled()
        page.get_by_role('button', name='Novo incidente').click()
        modal = page.get_by_role('dialog', name='Simular um incidente')
        expect(modal).to_be_visible()
        expect(modal.get_by_role('radio', name='Conexão recusada com o banco', exact=False)).to_be_checked()
        audits.append(audit(page, args.axe, 'scenario-dialog'))
        modal.get_by_role('button', name='Fechar', exact=True).focus()
        page.keyboard.press('Tab')
        assert page.evaluate("Boolean(document.activeElement.closest('[role=dialog]'))"), 'Modal focus escaped'
        page.keyboard.press('Escape')
        expect(page.get_by_role('button', name='Novo incidente')).to_be_focused()
        page.get_by_role('button', name='Novo incidente').click()
        modal.get_by_role('radio', name='Timeout de dependência', exact=False).check()
        modal.get_by_role('button', name='Injetar incidente', exact=True).click()
        expect(modal).not_to_be_visible()
        expect(page.get_by_text('Incidente criado. Selecione Analisar incidente para investigar.', exact=True)).to_be_visible()
        page.get_by_role('button', name='Analisar incidente', exact=True).click()
        expect(page.get_by_role('heading', name='Tempo limite excedido', exact=True)).to_be_visible(timeout=15000)
        expect(page.get_by_text('Nível Médio', exact=True)).to_be_visible()
        page.get_by_role('button', name='Pausar atualização', exact=True).click()
        page.get_by_role('tab', name='Evidências', exact=False).click()
        expect(page.get_by_text('Read timed out', exact=False).last).to_be_visible()
        page.get_by_role('tab', name='Ações', exact=True).click()
        expect(page.get_by_role('checkbox')).to_have_count(3)
        page.get_by_role('checkbox').first.check()
        expect(page.get_by_role('checkbox').first).to_be_checked()
        page.get_by_role('tab', name='Diagnóstico', exact=True).click()
        page.get_by_role('tab', name='Diagnóstico', exact=True).focus()
        page.keyboard.press('ArrowRight')
        expect(page.get_by_role('tab', name='Evidências', exact=False)).to_be_focused()
        page.get_by_role('tab', name='Diagnóstico', exact=True).click()
        with page.expect_download() as download:
            page.get_by_role('button', name='Exportar análise', exact=True).click()
        assert download.value.suggested_filename.endswith('.json')
        page.get_by_role('searchbox', name='Buscar incidentes').fill('no-matching-incident')
        expect(page.get_by_text('Nenhum incidente encontrado', exact=True)).to_be_visible()
        page.get_by_role('region', name='Fila de incidentes').get_by_role('button', name='Limpar filtros').click()
        page.get_by_role('button', name='Filtrar serviço checkout-api', exact=True).click()
        expect(page.get_by_role('button', name='Filtrar serviço checkout-api', exact=True)).to_have_attribute('aria-pressed', 'true')
        page.get_by_role('button', name='Remover filtro de serviço').click()
        # Low severity is a recovered operation, not an invented outage.
        page.get_by_role('button', name='Novo incidente').click()
        modal.get_by_role('radio', name='Cache recomposto', exact=False).check()
        modal.get_by_role('button', name='Injetar incidente', exact=True).click()
        expect(modal).not_to_be_visible()
        expect(page.get_by_text('Nível Leve', exact=True)).to_be_visible()
        page.get_by_role('button', name='Analisar incidente', exact=True).click()
        expect(page.get_by_role('heading', name='Cache recuperado por fallback', exact=True)).to_be_visible(timeout=15000)
        expect(page.get_by_text('Não há evidência de indisponibilidade', exact=False)).to_be_visible()
        page.get_by_label('Severidade', exact=True).select_option('LOW')
        expect(page.get_by_role('region', name='Fila de incidentes').get_by_role('button', name='Selecionar incidente', exact=False).first).to_be_visible()
        page.get_by_label('Severidade', exact=True).select_option('ALL')
        # Finish with the representative database scenario for the portfolio capture.
        page.get_by_role('button', name='Novo incidente').click()
        modal.get_by_role('radio', name='Conexão recusada com o banco', exact=False).check()
        modal.get_by_role('button', name='Injetar incidente', exact=True).click()
        expect(modal).not_to_be_visible()
        page.get_by_role('button', name='Analisar incidente', exact=True).click()
        expect(page.get_by_role('heading', name='Conectividade com o banco', exact=True)).to_be_visible(timeout=15000)
        page.evaluate('document.fonts.ready')
        audits.append(audit(page, args.axe, 'dark-desktop'))
        page.get_by_role('radio', name='Últimos 15 minutos').click()
        page.evaluate('window.scrollTo(0, 0)')
        page.screenshot(path=str(args.screenshot), full_page=True)
        page.get_by_role('button', name='Ativar tema claro').click()
        audits.append(audit(page, args.axe, 'light-desktop'))
        page.evaluate('window.scrollTo(0, 0)')
        page.screenshot(path=str(args.screenshot.with_name('dashboard-light.png')), full_page=True)
        page.reload()
        expect(page.locator('html')).to_have_class('light')
        page.get_by_role('button', name='Ativar tema escuro').click()
        page.set_viewport_size({'width': 390, 'height': 844})
        assert page.evaluate('document.documentElement.scrollWidth <= window.innerWidth'), 'Mobile page overflows'
        page.emulate_media(reduced_motion='reduce')
        assert page.evaluate('matchMedia("(prefers-reduced-motion: reduce)").matches')
        page.get_by_role('button', name='Abrir navegação').click()
        expect(page.get_by_role('navigation', name='Navegação principal')).to_be_visible()
        page.get_by_role('link', name='Visão geral', exact=True).click()
        expect(page.get_by_role('button', name='Abrir navegação')).to_be_visible()
        expect(page.get_by_role('region', name='Fila de incidentes').get_by_role('button', name='Selecionar incidente', exact=False).first).to_be_visible()
        audits.append(audit(page, args.axe, 'dark-mobile-reduced-motion'))
        page.screenshot(path=str(args.screenshot.with_name('dashboard-mobile.png')), full_page=True)
        # At a desktop-width browser, 200% zoom gives a comparable 800px layout viewport.
        page.set_viewport_size({'width': 800, 'height': 600})
        assert page.evaluate('document.documentElement.scrollWidth <= window.innerWidth'), '200% equivalent viewport overflows'
        page.set_viewport_size({'width': 1600, 'height': 1200})
        page.route('**/api/v1/incidents', lambda route: route.abort())
        page.get_by_role('button', name='Atualizar agora').click()
        expect(page.get_by_role('alert')).to_be_visible()
        page.unroute('**/api/v1/incidents')
        page.get_by_role('button', name='Tentar novamente').click()
        expect(page.get_by_role('alert')).not_to_be_visible()
        assert not errors, f'JavaScript page errors: {errors}'
        browser.close()
    report = {'browser_flow': 'passed', 'keyboard_dialog_and_tabs': 'passed', 'mobile_layout': 'passed', 'reduced_motion': 'passed', 'offline_retry': 'passed', 'page_errors': len(errors), 'axe': audits, 'screenshot': str(args.screenshot)}
    (args.screenshot.parent/'ui-report.json').write_text(json.dumps(report, indent=2) + '\n')
    print(json.dumps(report))


if __name__ == '__main__':
    main()
